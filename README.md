# @kako351/react-native-gemini-nano

Android 向けの React Native TurboModule ライブラリです。ML Kit Prompt API を通して、端末上の Gemini Nano を利用します。

## Requirements

- React Native 0.84 以上
- Android New Architecture 有効
- Android API 31 以上
- Gemini Nano Prompt API 対応端末

## Install

```bash
npm install @kako351/react-native-gemini-nano
```

Android 側は autolinking を前提にしています。

## Usage

```ts
import GeminiNano, {useGeminiNano} from '@kako351/react-native-gemini-nano';

const availability = await GeminiNano.getAvailability();

if (availability.status === 'needs_download') {
  await GeminiNano.downloadModel();
}

const text = await GeminiNano.generateText('3行で要約してください');
```

Hook を使う場合:

```ts
const {
  availability,
  refreshAvailability,
  downloadModel,
  generateText,
  generateTextStream,
  streamedText,
  isStreaming,
  downloadState,
} = useGeminiNano();
```

## API

- `isAvailable(): Promise<boolean>`
- `getAvailability(): Promise<{ status; isAvailable; message; errorCode? }>`
- `downloadModel(): Promise<void>`
- `generateText(prompt: string): Promise<string>`
- `generateTextStream(prompt: string): void`

`getAvailability().status` は次を返します。

- `available`
- `needs_download`
- `downloading`
- `needs_system_update`
- `busy`
- `unavailable`
- `unknown`

## Notes

- 実行基盤は ML Kit Prompt API ですが、実際のオンデバイス推論は AICore / Gemini Nano により行われます。
- 端末が対応機種でも、モデル未配信やダウンロード未完了の間は `available` にならないことがあります。
- サンプルアプリは `example/` に入っています。
