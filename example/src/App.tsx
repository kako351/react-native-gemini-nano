import React, {useEffect, useState} from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {useGeminiNano} from '@kako351/react-native-gemini-nano';

const DEFAULT_PROMPT = 'こんにちは';

function App(): React.JSX.Element {
  const [prompt, setPrompt] = useState(DEFAULT_PROMPT);
  const [mode, setMode] = useState<'single' | 'stream' | null>(null);

  const {
    availability,
    downloadModel,
    downloadState,
    refreshAvailability,
    generateText,
    generateTextStream,
    isCheckingAvailability,
    isDownloadingModel,
    isStreaming,
    streamedText,
    error,
  } = useGeminiNano();

  const [singleResult, setSingleResult] = useState('');

  useEffect(() => {
    void refreshAvailability();
    // 初回表示時だけ AICore の準備状態を確認する。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (availability?.status !== 'downloading') {
      return;
    }

    const intervalId = setInterval(() => {
      void refreshAvailability();
    }, 3000);

    return () => {
      clearInterval(intervalId);
    };
  }, [availability?.status, refreshAvailability]);

  const handleGenerate = async () => {
    setMode('single');
    setSingleResult('');

    try {
      const result = await generateText(prompt);
      setSingleResult(result);
    } catch (nextError) {
      setSingleResult(
        nextError instanceof Error ? nextError.message : '生成に失敗しました。',
      );
    }
  };

  const handleStream = () => {
    setMode('stream');
    generateTextStream(prompt);
  };

  const handleDownloadModel = async () => {
    try {
      await downloadModel();
    } catch (nextError) {
      setSingleResult(
        nextError instanceof Error
          ? nextError.message
          : 'モデルのダウンロードに失敗しました。',
      );
    }
  };

  const canGenerate = availability?.isAvailable === true && !isStreaming;
  const canDownload =
    availability?.status === 'needs_download' && !isDownloadingModel;

  const responseText =
    mode === 'stream'
      ? streamedText || (isStreaming ? '生成中...' : 'まだ実行していません。')
      : singleResult ||
        availability?.message ||
        (isCheckingAvailability ? 'AICore の状態を確認中...' : 'まだ実行していません。');

  const topInset = (StatusBar.currentHeight ?? 0) + 12;

  return (
    <View style={styles.screen}>
      <StatusBar barStyle="light-content" />
      <ScrollView contentContainerStyle={[styles.content, {paddingTop: topInset}]}>
        <Text style={styles.title}>Gemini Nano Example</Text>

        <View style={styles.panel}>
          <Text style={styles.label}>Prompt</Text>
          <TextInput
            multiline
            value={prompt}
            onChangeText={setPrompt}
            style={styles.input}
            placeholder="Gemini Nano に送るプロンプト"
            placeholderTextColor="#7b8794"
          />

          <View style={styles.buttonRow}>
            <TouchableOpacity
              style={[
                styles.button,
                styles.primaryButton,
                !canGenerate && styles.disabledButton,
              ]}
              onPress={handleGenerate}
              disabled={!canGenerate}>
              <Text style={styles.primaryButtonText}>単発生成</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.button,
                styles.secondaryButton,
                !canGenerate && styles.disabledButton,
              ]}
              onPress={handleStream}
              disabled={!canGenerate}>
              <Text style={styles.secondaryButtonText}>
                {isStreaming ? '生成中...' : 'ストリーム生成'}
              </Text>
            </TouchableOpacity>
          </View>

          {availability ? (
            <Text
              style={[
                styles.availabilityText,
                availability.isAvailable
                  ? styles.availabilityOkText
                  : styles.availabilityWarningText,
              ]}>
              {availability.message}
            </Text>
          ) : null}

          {availability?.status === 'needs_download' ? (
            <TouchableOpacity
              style={[
                styles.button,
                styles.downloadButton,
                !canDownload && styles.disabledButton,
              ]}
              onPress={handleDownloadModel}
              disabled={!canDownload}>
              <Text style={styles.downloadButtonText}>
                {isDownloadingModel ? 'ダウンロード中...' : 'モデルをダウンロード'}
              </Text>
            </TouchableOpacity>
          ) : null}

          {availability?.status === 'downloading' ? (
            <Text style={styles.downloadText}>
              Gemini Nano のモデルをダウンロード中です。完了後に自動で利用可能になります。
            </Text>
          ) : null}
        </View>

        <View style={styles.panel}>
          <Text style={styles.label}>Response</Text>
          {isStreaming || isDownloadingModel ? (
            <ActivityIndicator color="#0f172a" />
          ) : null}
          <Text style={styles.responseText}>{responseText}</Text>
          {downloadState ? (
            <Text style={styles.downloadText}>
              {renderDownloadState(downloadState)}
            </Text>
          ) : null}
          {error ? (
            <Text style={styles.errorText}>
              {error.code}: {error.message}
            </Text>
          ) : null}
        </View>
      </ScrollView>
    </View>
  );
}

function renderDownloadState(
  state: NonNullable<ReturnType<typeof useGeminiNano>['downloadState']>,
): string {
  switch (state.status) {
    case 'pending':
      return 'ダウンロード待機中です。';
    case 'started':
      return `ダウンロードを開始しました。総サイズ: ${formatBytes(
        state.totalBytes,
      )}`;
    case 'in_progress':
      return `ダウンロード中: ${formatBytes(
        state.bytesDownloaded,
      )} / ${formatBytes(state.totalBytes)}`;
    case 'completed':
      return 'モデルのダウンロードが完了しました。';
    case 'failed':
      return state.message ?? 'モデルのダウンロードに失敗しました。';
    default:
      return '';
  }
}

function formatBytes(bytes?: number): string {
  if (bytes == null || Number.isNaN(bytes)) {
    return '-';
  }

  if (bytes < 1024 * 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#08111d',
  },
  content: {
    padding: 20,
    gap: 16,
  },
  title: {
    color: '#f8fafc',
    fontSize: 30,
    fontWeight: '700',
  },
  panel: {
    backgroundColor: '#f8fafc',
    borderRadius: 20,
    padding: 16,
    gap: 12,
  },
  label: {
    color: '#0f172a',
    fontSize: 14,
    fontWeight: '700',
  },
  input: {
    minHeight: 180,
    borderRadius: 16,
    backgroundColor: '#e2e8f0',
    color: '#0f172a',
    padding: 14,
    textAlignVertical: 'top',
    fontSize: 15,
    lineHeight: 22,
  },
  buttonRow: {
    flexDirection: 'row',
    gap: 12,
  },
  button: {
    flex: 1,
    minHeight: 52,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 12,
  },
  primaryButton: {
    backgroundColor: '#0f172a',
  },
  secondaryButton: {
    backgroundColor: '#dbeafe',
  },
  disabledButton: {
    opacity: 0.5,
  },
  downloadButton: {
    backgroundColor: '#dcfce7',
  },
  downloadButtonText: {
    color: '#166534',
    fontSize: 14,
    fontWeight: '700',
  },
  primaryButtonText: {
    color: '#f8fafc',
    fontSize: 14,
    fontWeight: '700',
  },
  secondaryButtonText: {
    color: '#1d4ed8',
    fontSize: 14,
    fontWeight: '700',
  },
  responseText: {
    color: '#0f172a',
    fontSize: 15,
    lineHeight: 22,
  },
  downloadText: {
    color: '#1d4ed8',
    fontSize: 13,
    lineHeight: 18,
  },
  availabilityText: {
    fontSize: 13,
    lineHeight: 18,
  },
  availabilityOkText: {
    color: '#166534',
  },
  availabilityWarningText: {
    color: '#92400e',
  },
  errorText: {
    color: '#b91c1c',
    fontSize: 14,
    fontWeight: '600',
  },
});

export default App;
