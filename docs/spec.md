# Particle Muffler 仕様書

## 1. 概要

Particle Muffler は、設置されたブロックの周囲で発生する Minecraft のパーティクルをクライアント側で抑制する Mod である。

Sound Muffler 系 Mod が周囲の音を抑制するのに対し、この Mod は周囲のパーティクルを抑制する。主目的は装飾制御ではなく、工業Mod、魔術Mod、Mobファーム、農業設備などで大量発生するパーティクルによる負荷を局所的に下げることである。

初期実装では細かい粒子タイプ制御は行わず、特定範囲内の全パーティクルを高速に無効化する。

## 2. 対象環境

初期実装の対象は以下とする。

```text
Minecraft: 1.20.1
Mod framework: Architectury
Loaders:
  - Fabric
  - Forge
License:
  - LGPL-3.0-only または LGPL-3.0-or-later
Language:
  - Java
```

## 3. 基本方針

パーティクルは基本的にクライアント側の視覚効果であるため、サーバー側で粒子発生源を止めるのではなく、クライアント側のパーティクル生成処理にMixinし、抑制対象であれば生成をキャンセルする。

基本フローは以下。

```text
ParticleEngine#createParticle が呼ばれる
↓
Particle Muffler の有効領域が存在するか確認
↓
粒子発生座標から section key を計算
↓
その section が抑制対象なら createParticle の戻り値を null にする
↓
パーティクルは生成されない
```

## 4. 重要な設計判断

### 4.1 Mixin順序

Particle Core、Sodium Extra、Embeddium Extra などのグローバル粒子無効化Modは、粒子タイプや設定値を見るだけの軽い判定を先に行う可能性が高い。

Particle Muffler は座標または section 判定を行うため、それらより後に動く方が妥当である。

したがって、初期値は以下とする。

```java
@Mixin(value = ParticleEngine.class, priority = 1100)
```

```java
@Inject(
    method = "createParticle",
    at = @At("HEAD"),
    cancellable = true,
    order = 1100
)
```

意図する順序は以下。

```text
Particle Core / Sodium Extra などの軽いグローバル粒子判定
↓
Particle Muffler の section 判定
↓
Particle Core / Particle Culling などの描画時・視界判定系
↓
通常のパーティクル生成・描画
```

完全な順序保証はできないが、デフォルト `1000` のMixinより後に回ることを狙う。

### 4.2 抑制単位

初期実装では、正確な球形・立方体範囲ではなく、`SectionPos` ベースで抑制する。

Minecraftのsectionは 16x16x16 ブロック単位である。

理由:

```text
- 粒子生成ごとの判定を非常に軽くできる
- Muffler数に依存しにくい
- 座標ベースの複数entry contains判定を避けられる
- このModの用途は細かい制御ではなく局所的な軽量化である
```

粒子生成時の判定は以下を目標にする。

```text
hasAnyActiveMuffler check
↓
SectionPos key calculation
↓
Long2IntMap or LongSet contains
```

## 5. MVP仕様

MVPで実装するものは以下。

```text
- Particle Muffler ブロック
- Particle Muffler BlockEntity
- section単位の全パーティクル抑制
- Redstone信号による有効/無効制御
- クライアント側レジストリ
- ParticleEngine#createParticle へのMixin
- Fabric / Forge 両対応
- configによる基本設定
```

MVPで実装しないもの。

```text
- 粒子タイプ別フィルタ
- GUI
- 範囲可視化
- アップグレードアイテム
- 球形/正確な立方体範囲判定
- 花火やブロック破壊粒子などの特殊経路への追加Mixin
```

## 6. ブロック仕様

### 6.1 Particle Muffler

基本版ブロック。

```text
Name: Particle Muffler
Purpose: 周囲のsection内の全パーティクルを抑制する
Control: Redstone信号で無効化
Range unit: section
Filter: なし
```

挙動:

```text
Redstone信号なし:
  有効

Redstone信号あり:
  無効
```

ゲーム内説明文案:

```text
Suppresses all particles in nearby sections.
Fastest option for performance-focused setups.
```

日本語説明文案:

```text
周囲の区画内の全パーティクルを抑制します。
軽量化重視の設備向けです。
```

## 7. 将来追加するブロック

MVP後に、必要であれば以下を追加する。

### 7.1 Filtered Particle Muffler

フィルタ付き上位版。

```text
Name: Filtered Particle Muffler
Purpose: 指定した粒子だけ消す、または指定した粒子だけ残す
Control: Redstone信号で無効化
Range unit: section
Filter: blacklist / whitelist
```

フィルタモード:

```java
public enum FilterMode {
    BLACKLIST,
    WHITELIST
}
```

意味:

```text
BLACKLIST:
  リストに含まれる粒子だけ抑制する

WHITELIST:
  リストに含まれる粒子だけ残し、それ以外を抑制する
```

基本版の Particle Muffler には粒子タイプ判定を入れない。
基本版は最速実装を維持する。

## 8. 内部データ構造

### 8.1 ClientMufflerEntry

MVPでは、抑制対象のsectionを事前に展開するため、粒子生成時に個別のMuffler entryを走査しない。

ただし、BlockEntity管理用に以下のようなentryを持ってよい。

```java
public record ClientMufflerEntry(
    BlockPos pos,
    int sectionRadius,
    boolean enabled
) {
}
```

### 8.2 Client Registry

クライアント側に、現在のワールドに存在するParticle Mufflerの抑制sectionを保持する。

推奨構造:

```java
public final class ParticleMufflerClientRegistry {
    private static boolean hasAnyActiveMuffler;

    private static final Long2IntMap suppressedSectionRefCounts =
        new Long2IntOpenHashMap();

    private static final Map<BlockPos, ClientMufflerEntry> entriesByPos =
        new HashMap<>();
}
```

`Long2IntMap` には fastutil を使う想定。

理由:

```text
- 同じsectionを複数のMufflerが抑制する可能性がある
- 片方のMuffler撤去時に、他方の効果まで消さないため参照カウントが必要
```

### 8.3 section ref count

section登録時:

```java
private static void addSuppressedSection(long key) {
    suppressedSectionRefCounts.addTo(key, 1);
    hasAnyActiveMuffler = !suppressedSectionRefCounts.isEmpty();
}
```

section解除時:

```java
private static void removeSuppressedSection(long key) {
    int oldCount = suppressedSectionRefCounts.get(key);

    if (oldCount <= 1) {
        suppressedSectionRefCounts.remove(key);
    } else {
        suppressedSectionRefCounts.put(key, oldCount - 1);
    }

    hasAnyActiveMuffler = !suppressedSectionRefCounts.isEmpty();
}
```

## 9. 抑制範囲

MVPでは `sectionRadius` を使用する。

デフォルト:

```text
sectionRadius = 0
```

これは Particle Muffler が設置された section のみを抑制する。

つまり、デフォルトでは以下の範囲。

```text
1 * 16 = 16 blocks per axis
```

ただし、これは正確な半径16ブロックではなく、section単位で丸ごと抑制される。
`sectionRadius = 1` の場合は、中心sectionを含めて周囲 3x3x3 sections が対象になる。

configで指定可能にする。

```toml
[general]
defaultSectionRadius = 0
maxSectionRadius = 2
redstoneDisables = true
```

`maxSectionRadius = 2` の場合、5x5x5 sections が対象になる。

## 10. BlockEntity仕様

`ParticleMufflerBlockEntity` は以下を持つ。

```java
private int sectionRadius;
private boolean enabled;
```

初期値:

```java
sectionRadius = config.defaultSectionRadius;
enabled = true;
```

保存対象:

```text
- sectionRadius
- enabled
```

NBT保存・読み込みを実装する。

## 11. Redstone制御

`ParticleMufflerBlock` でneighbor updateを受け取り、Redstone入力状態に応じてBlockEntityの `enabled` を更新する。

仕様:

```text
redstoneDisables = true の場合:
  level.hasNeighborSignal(pos) == true なら enabled = false
  level.hasNeighborSignal(pos) == false なら enabled = true
```

Redstone状態が変わった場合はクライアントに同期し、クライアント側レジストリを更新する。

## 12. 同期仕様

Particle Mufflerはクライアント側で粒子生成をキャンセルするため、クライアント側が以下を把握している必要がある。

```text
- MufflerのBlockPos
- sectionRadius
- enabled
```

同期タイミング:

```text
- ブロック設置時
- チャンク読み込み時
- BlockEntity更新時
- Redstone入力変化時
- ブロック撤去時
- チャンクアンロード時
```

実装方針:

```text
MVPでは、BlockEntityの通常同期パケットを使う。
必要であればArchitectury Networkingで独自パケットを追加する。
```

クライアント同期後、以下を行う。

```text
enabled = true:
  ParticleMufflerClientRegistry.addOrUpdate(pos, sectionRadius, enabled)

enabled = false:
  ParticleMufflerClientRegistry.remove(pos)
```

ブロック撤去時・チャンクアンロード時も `remove(pos)` を呼ぶ。

## 13. Client Registry API

最低限、以下のAPIを実装する。

```java
public final class ParticleMufflerClientRegistry {
    public static boolean hasAnyActiveMuffler();

    public static void addOrUpdate(BlockPos pos, int sectionRadius, boolean enabled);

    public static void remove(BlockPos pos);

    public static void clear();

    public static boolean isSuppressedFast(double x, double y, double z);
}
```

### 13.1 addOrUpdate

処理:

```text
1. 既存entryがある場合は remove(pos) で古いsection ref countを解除
2. enabled == false なら何も登録しない
3. posから中心sectionを計算
4. sectionRadius内のsection keyを列挙
5. suppressedSectionRefCounts に加算
6. entriesByPos に保存
7. hasAnyActiveMuffler を更新
```

### 13.2 remove

処理:

```text
1. entriesByPos からentryを取得
2. なければ何もしない
3. entryのsectionRadiusから対象section keyを再計算
4. suppressedSectionRefCounts から減算
5. entriesByPos から削除
6. hasAnyActiveMuffler を更新
```

### 13.3 clear

ワールド離脱・サーバー切断・ディメンション変更時に呼ぶ。
Architectury の client lifecycle / client player event で level load、client stopping、client player quit を捕捉し、registry を clear する。
同一ワールド内のチャンクアンロード漏れに備えて、低頻度の client level tick で存在しない BlockEntity entry を pruning してよい。

```text
entriesByPos.clear()
suppressedSectionRefCounts.clear()
hasAnyActiveMuffler = false
```

### 13.4 isSuppressedFast

粒子生成時に毎回呼ばれるため、極力軽くする。

```java
public static boolean isSuppressedFast(double x, double y, double z) {
    if (!hasAnyActiveMuffler) {
        return false;
    }

    int sectionX = Mth.floor(x) >> 4;
    int sectionY = Mth.floor(y) >> 4;
    int sectionZ = Mth.floor(z) >> 4;

    long key = SectionPos.asLong(sectionX, sectionY, sectionZ);

    return suppressedSectionRefCounts.containsKey(key);
}
```

## 14. Mixin仕様

### 14.1 ParticleEngineMixin

対象:

```text
net.minecraft.client.particle.ParticleEngine
```

対象メソッド:

```text
createParticle(
    ParticleOptions options,
    double x,
    double y,
    double z,
    double velocityX,
    double velocityY,
    double velocityZ
)
```

目的:

```text
sectionが抑制対象ならパーティクル生成をキャンセルする。
```

実装イメージ:

```java
@Mixin(value = ParticleEngine.class, priority = 1100)
public class ParticleEngineMixin {
    @Inject(
        method = "createParticle",
        at = @At("HEAD"),
        cancellable = true,
        order = 1100
    )
    private void particlemuffler$suppressParticle(
        ParticleOptions options,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ,
        CallbackInfoReturnable<Particle> cir
    ) {
        if (ParticleMufflerClientRegistry.isSuppressedFast(x, y, z)) {
            cir.setReturnValue(null);
        }
    }
}
```

補足:

```text
- options.getType() はMVPでは呼ばない
- ParticleType ID判定はしない
- Minecraft.getInstance().level も可能なら毎回参照しない
- Registryが空なら boolean check だけでreturnする
```

## 15. Config仕様

MVPで必要なconfig。

```toml
[general]
defaultSectionRadius = 0
maxSectionRadius = 2
redstoneDisables = true

[client]
pruningIntervalTicks = 20

[compat]
particleEngineMixinOrder = 1100
particleEngineMixinPriority = 1100
```

`pruningIntervalTicks` は、クライアント側registryから存在しないBlockEntity entryを掃除する間隔。
TOMLファイルは `config/particlemuffler.toml` に生成する。

ただし、Mixinの `order` / `priority` は通常のゲーム内configで動的変更できない。
そのため、MVPではコード定数として `1100` を使用してよい。

将来的に切り替えたい場合は、別mixin configまたは起動前configで対応する。

## 16. Architectury構成

推奨構成。

```text
particle-muffler/
  common/
    src/main/java/.../
      ParticleMuffler.java
      registry/
        ModBlocks.java
        ModBlockEntities.java
        ModItems.java
      block/
        ParticleMufflerBlock.java
      blockentity/
        ParticleMufflerBlockEntity.java
      client/
        ParticleMufflerClientRegistry.java
        ClientMufflerEntry.java
      config/
        ParticleMufflerConfig.java
      mixin/
        ParticleEngineMixin.java

  fabric/
    src/main/java/.../
      ParticleMufflerFabric.java
      ParticleMufflerClientFabric.java

  forge/
    src/main/java/.../
      ParticleMufflerForge.java
      ParticleMufflerClientForge.java
```

## 17. 登録

MVPで登録するもの。

```text
- Particle Muffler block
- Particle Muffler block item
- Particle Muffler block entity type
- creative tab entry
```

ArchitecturyのDeferredRegister等を使ってcommon側に寄せる。

## 18. 動作確認

### 18.1 基本確認

以下のコマンドで確認する。

```mcfunction
/particle minecraft:flame ~ ~ ~ 1 1 1 0 100
/particle minecraft:smoke ~ ~ ~ 1 1 1 0 100
/particle minecraft:portal ~ ~ ~ 1 1 1 0 100
```

確認項目:

```text
- Particle Muffler の有効section内では粒子が表示されない
- 対象section外では粒子が表示される
- Redstone信号入力中は粒子が表示される
- Redstone信号解除後は再び粒子が抑制される
- ブロック撤去後は粒子が表示される
- チャンク再読み込み後も状態が正しい
- ディメンション移動後に古いregistryが残らない
```

### 18.2 通常パーティクル確認

確認対象:

```text
- torch
- campfire
- smoke
- portal
- potion
- villager heart / angry
- mob attack particles
- block break particles
- modded machine particles
```

MVPでは、`ParticleEngine#createParticle` を通らない特殊粒子が残ってもよい。
その場合は後続タスクとして記録する。

## 19. パフォーマンス要件

粒子生成時の追加コストは最小限にする。

必須条件:

```text
- Mufflerが存在しない場合、boolean分岐1回程度でreturnする
- 粒子タイプIDはMVPでは取得しない
- ResourceLocation lookupをしない
- Muffler entryの線形走査をしない
- section key lookupのみで判定する
```

避けるべき実装:

```java
for (ClientMufflerEntry entry : allMufflers) {
    if (entry.contains(x, y, z)) {
        return true;
    }
}
```

採用する実装:

```java
long key = SectionPos.asLong(sectionX, sectionY, sectionZ);
return suppressedSectionRefCounts.containsKey(key);
```

## 20. 互換性方針

想定する併用Mod:

```text
- Particle Core
- Sodium Extra
- Embeddium Extra
- Sodium
- Embeddium
- Iris
- Oculus
- Particle Culling 系Mod
```

方針:

```text
- Particle Core / Sodium Extra 系の軽いグローバル判定より後に実行する
- 描画カリング系より前に粒子生成を止める
- rendererには直接Mixinしない
- ParticleEngine#createParticle へのHEAD injectionを主経路とする
```

初期Mixin順:

```text
priority = 1100
order = 1100
```

## 21. ライセンス表記

READMEに以下のような記述を入れる。

```text
This project is licensed under the GNU Lesser General Public License v3.0.

Particle suppression behavior was designed with reference to Sodium Extra's particle suppression approach.
Block-based area suppression behavior was inspired by Sound Muffler-style mods.

No source code from Sodium Extra is included unless explicitly noted in file headers.
```

Sodium Extra等のコードを直接コピーしないこと。
コードをコピーまたは改変した場合は、該当ファイルに由来、著作権表示、変更内容を明記する。

## 22. MVP完了条件

MVPの完了条件は以下。

```text
- Minecraft 1.20.1で起動できる
- Fabricで動作する
- Forgeで動作する
- Particle Mufflerブロックを設置できる
- BlockEntityが状態を保存する
- sectionRadiusとenabledがクライアントに同期される
- 有効section内のParticleEngine#createParticle由来の粒子が生成されない
- Redstone信号で無効化できる
- ブロック撤去時にregistryから削除される
- ワールド離脱時にregistryがclearされる
```

## 23. 実装順序

Codexは以下の順序で実装する。

```text
1. Architectury 1.20.1 プロジェクト構成を確認する
2. LGPL LICENSE を追加する
3. ModBlocks / ModItems / ModBlockEntities を作る
4. ParticleMufflerBlock を作る
5. ParticleMufflerBlockEntity を作る
6. NBT保存・読み込みを実装する
7. Redstone制御を実装する
8. BlockEntity同期を実装する
9. ParticleMufflerClientRegistry を実装する
10. ParticleEngineMixin を実装する
11. Fabric / Forge 両方で起動確認する
12. /particle コマンドで抑制動作を確認する
13. ブロック撤去・チャンク再読み込み・ワールド離脱時のregistry cleanupを確認する
```

## 24. 非MVPタスク

以下はMVP後に実装する。

```text
- Filtered Particle Muffler
- BLACKLIST / WHITELIST
- GUI
- 粒子ID選択UI
- 最近見た粒子リスト
- 範囲可視化
- ClientLevel#addDestroyBlockEffect 対応
- ClientLevel#addBreakingBlockEffect 対応
- FireworkParticles 対応
- Weather particle 対応
- 起動前configによるmixin order切り替え
```

## 25. 最終設計まとめ

MVPの本質は以下。

```text
Particle Muffler Block
+ BlockEntity(sectionRadius, enabled)
+ client-side section registry
+ Long2IntMap ref count
+ ParticleEngine#createParticle HEAD Mixin
+ order/priority 1100
+ Redstone disables
```

このModは細かい粒子管理Modではなく、局所的にパーティクル負荷を下げるための軽量化ブロックである。

そのため、初期実装では粒子タイプ判定を行わず、section単位で全パーティクルを高速に抑制する。
