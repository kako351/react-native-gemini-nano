# Gemini Nano Example

`@kako351/react-native-gemini-nano` を Android の React Native アプリから利用するためのサンプルです。

## セットアップ

1. `cd example`
2. `npm install`
3. `npm start`
4. 別ターミナルで `npm run android`

## 補足

- `example/android/gradlew` と `gradle-wrapper.jar` は追加済みです。
- ライブラリは npm から `@kako351/react-native-gemini-nano` をインストールします。
- ルートからは `npm run example:install` / `npm run example:start` / `npm run example:android` でも実行できます。

## 前提

- Android 端末またはエミュレータで React Native 0.84 が動作すること
- Android 12 以上 (`minSdk 31`) の端末またはエミュレータであること
- AICore と Gemini Nano が利用可能な端末であること
- New Architecture が有効であること

## 確認できること

- `isAvailable()` による利用可否確認
- `generateText()` による単発テキスト生成
- `useGeminiNano()` を通したストリーミング生成
- AICore / モデル準備前提の確認 UI
