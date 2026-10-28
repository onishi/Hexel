# Hexel

**Your apps. Your space.** — *Flick to move. Swipe to launch.*

正六角形のタイルを敷き詰めた一枚のマップにアプリを並べる、Android 向けホームランチャー（MVP）。

- タップ … アプリ起動
- フリック（素早く弾く） … マップを慣性スクロール
- スワイプ（指をつけたまましっかり引っ張る） … 6方向それぞれに割り当てたアプリを起動
- 長押し … そのアプリの6方向の割り当てを編集

---

## 構成

```
core/   純Kotlin（Android非依存）。JVMで単体テスト可能
  hex/        Axial座標・Pointy-top レイアウト・6方向・螺旋列挙
  gesture/    フリック／スワイプ判定（GestureClassifier）と閾値（GestureConfig）
  layout/     アプリの自動配置（SlotAllocator）
  model/      HexApp / GestureAssignment
app/    Android アプリ（Jetpack Compose）
  apps/       インストール済みアプリ取得（LauncherApps）・起動（AppLauncher）
  data/       DataStore 永続化（配置・ジェスチャー割り当て）
  ui/home/    ヘックスマップ描画（Canvas）・ジェスチャー処理・カメラ（慣性スクロール）
  ui/editor/  割り当て編集画面・アプリ選択画面
  ui/theme/   Paper テーマ
```

責務の分離：座標計算＝`core/hex`、判定＝`core/gesture`、描画＝`HexMapRenderer`、
タッチイベント処理＝`HexMapGestures`、起動＝`AppLauncher`、保存＝`HexelStore`。

---

## ビルド手順

必要なもの：Android Studio（Ladybug 2024.2 以降推奨）、JDK 17 以上（Android Studio 同梱の JBR で可）、Android SDK 35。

### Android Studio

1. `File > Open` でこのリポジトリのルートを開く
2. Gradle Sync の完了を待つ
3. 実行構成 `app` を選び ▶ Run

### コマンドライン

```bash
./gradlew :core:test          # 座標・判定・配置の単体テスト（JVMのみ、SDK不要）
./gradlew :app:assembleDebug  # app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` に `sdk.dir=/path/to/Android/sdk` が必要です（Android Studio で開けば自動生成）。

---

## 実機インストール手順

1. 端末で「開発者向けオプション」→「USB デバッグ」を有効にし、PC と接続
2. インストール
   ```bash
   ./gradlew :app:installDebug
   # または
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. ホームボタンを押し、ホームアプリの選択で **Hexel** を選んで「常時」
   - 選択ダイアログが出ない場合：設定 → アプリ → デフォルトのアプリ → ホームアプリ → Hexel
4. 元のランチャーに戻すときは同じ設定画面から変更（Hexel のマップには設定アプリも並ぶので、そこから開けます）。
   アンインストールは `adb uninstall com.hexel.launcher`

---

## ジェスチャー検証手順

デバッグビルドでは判定ごとにログが出ます。

```bash
adb logcat -s HexelGesture
```

出力例：

```
Flick(velocity=Vec2(x=12.0, y=-1830.0))  dur=96ms dist=84.3dp max=84.3dp path=85.1dp v=1830dp/s
Swipe(direction=NORTH_EAST)  dur=412ms dist=71.0dp max=73.2dp path=80.4dp v=95dp/s
Cancel(reason=returned to start)  dur=640ms dist=12.4dp max=76.0dp path=150.2dp v=40dp/s
```

### 準備

1. 任意のアプリ（例：カメラ）を長押し → 編集画面で「右上」をタップ → 別アプリ（例：時計）を選択
2. 同様に数方向へ割り当て、1方向は未割り当てのまま残す

### チェックリスト

| # | 操作 | 期待結果 |
|---|------|----------|
| 1 | アプリのタイルをタップ | 即起動 |
| 2 | 空きタイルをタップ | 何も起きない |
| 3 | 空きタイルからゆっくりドラッグ | マップが指に追従 |
| 4 | アプリのタイル上で素早く弾く（各方向） | マップが慣性スクロール。ハイライトは出ない。`Flick` |
| 5 | アプリのタイルから右上へしっかり引っ張る | 右上の辺が淡い青、隣のタイルに起動先アプリが表示。離すと起動。`Swipe` |
| 6 | 5 の状態から指を戻して離す | ハイライトが消え、何も起動しない。`Cancel(returned to start)` |
| 7 | 未割り当ての方向へ引っ張る | 灰色の楔のみ表示、離しても何も起動しない |
| 8 | 引っ張りながら方向を変える | ハイライトが追従（方向が変わるたびに軽い振動） |
| 9 | 引っ張った後、最後に勢いよく投げる | 起動せずスクロール。`Flick` |
| 10 | アプリのタイルから大きく（約240dp超）ドラッグ | 何も起動しない。`Cancel(too far)` |
| 11 | スクロール中のマップに触れる | 止まるだけで起動しない |
| 12 | 長押し | 振動して編集画面が開く |
| 13 | 2本指で触れる | 何も起動しない |
| 14 | アプリ起動 → ホームに戻る | マップ位置がそのまま |
| 15 | 端末再起動 | 割り当て・配置が維持されている |
| 16 | アプリをインストール／アンインストール | タイルが追加／空きになる。他のアプリの位置は変わらない |

### 閾値の調整

すべて `core/src/main/kotlin/com/hexel/core/gesture/GestureConfig.kt` にあります（dp・ms 単位）。

| 項目 | 初期値 | 意味 |
|------|-------:|------|
| `touchSlopDp` | 10 | これ以下の移動はタップ／長押し扱い |
| `longPressMs` | 450 | 静止したままこの時間で長押し |
| `flickMaxDurationMs` / `flickMinVelocityDpPerSec` | 250 / 650 | この時間内・この速度以上で離せばフリック |
| `swipeMaxReleaseVelocityDpPerSec` | 2000 | 離す瞬間がこれ以上速ければ常にフリック |
| `swipeMinDurationMs` | 120 | これより短い操作はスワイプにならない |
| `swipeMinDistanceDp` | 48 | 離した位置がこれ以上離れていればスワイプ（≒隣のタイルに入る距離） |
| `swipeMaxDistanceDp` | 240 | これを超えるとキャンセル（マップを動かそうとした操作とみなす） |
| `previewDelayMs` | 120 | これ以降にだけハイライトを表示（フリックでちらつかせない） |

判定順：①短時間かつ高速 → フリック ②離す瞬間が非常に速い → フリック ③短すぎる → フリック
④離した位置が近い → キャンセル ⑤遠すぎる → キャンセル ⑥それ以外 → その方向へスワイプ。
迷う操作は「スワイプ以外」に倒しています（誤スクロールより誤起動のほうがコストが高いため）。

ログを見ながら値を変え、`./gradlew :core:test` でテストが通ることを確認してから実機で再検証してください。

---

## 仕様上の判断（不明点に対する初期値）

- **ジェスチャーの扱い**：アプリのタイルから始まる操作中はマップを動かしません（スワイプのプレビューを安定させるため）。フリックと判定されたら、離した瞬間の速度で慣性スクロールを開始します。マップを指で追従させたいときは空きタイルから。
- **初期配置**：アプリ名順（ロケール対応の照合順）に中心から螺旋状に配置。配置は保存され、新規アプリは中心に近い空きタイルへ入り、アンインストールしたアプリのタイルは空きになります（既存アプリの位置は動かない）。
- **1パッケージ1タイル**：複数のランチャーアクティビティを持つアプリは最初のもののみ。仕事用プロファイルは対象外。Hexel 自身は表示しません。
- **スクロール範囲**：最も外側のアプリが画面中央に来るところまで。
- **タイルサイズ**：半径 44dp（幅 76dp）、アイコン 38dp、アプリ名 10sp。`ui/home/TileMetrics.kt` で調整可。
- **フォント**：システムの sans-serif（英数字は Roboto、日本語は Noto Sans CJK）。Inter を使う場合は `res/font` に追加し `ui/theme/Theme.kt` の `HexelFont` を差し替え。
- **画面向き**：縦固定（`screenOrientation="portrait"`）。
- **戻るボタン**：ホームでは無視、編集画面では一階層戻る。ホームボタンで編集画面を閉じる。
- **割り当て先がアンインストールされた場合**：割り当ては残し（再インストールで復活）、起動・プレビューはしません。編集画面では「（未インストール）」と表示。
- **マップ位置**：ViewModel に保持（アプリ起動・画面回転では維持）。プロセスが破棄された場合は中央に戻ります。

## 動作要件

minSdk 26（Android 8.0、Adaptive Icon 対応のため）／ targetSdk 35。
