import { type GeminiNanoAvailability } from './NativeGeminiNano';
export declare const GEMINI_NANO_STREAM_CHUNK_EVENT = "GeminiNano:onTextChunk";
export declare const GEMINI_NANO_STREAM_END_EVENT = "GeminiNano:onTextStreamEnd";
export declare const GEMINI_NANO_STREAM_ERROR_EVENT = "GeminiNano:onTextStreamError";
export declare const GEMINI_NANO_DOWNLOAD_EVENT = "GeminiNano:onDownloadStatus";
export type GeminiNanoStreamChunkEvent = {
    chunk: string;
};
export type GeminiNanoStreamEndEvent = {
    fullText?: string;
};
export type GeminiNanoStreamErrorEvent = {
    code: string;
    message: string;
};
export type GeminiNanoDownloadEvent = {
    status: 'pending' | 'started' | 'in_progress' | 'completed' | 'failed';
    bytesDownloaded?: number;
    totalBytes?: number;
    code?: string;
    message?: string;
};
type StreamHandlers = {
    onChunk?: (chunk: string) => void;
    onComplete?: (fullText: string) => void;
    onError?: (error: GeminiNanoStreamErrorEvent) => void;
};
type DownloadHandlers = {
    onStatus?: (event: GeminiNanoDownloadEvent) => void;
};
export declare const GeminiNano: {
    isAvailable(): Promise<boolean>;
    getAvailability(): Promise<GeminiNanoAvailability>;
    downloadModel(handlers?: DownloadHandlers): Promise<void>;
    generateText(prompt: string): Promise<string>;
    generateTextStream(prompt: string, handlers?: StreamHandlers): () => void;
    addStreamingListeners: ({ onChunk, onComplete, onError, }: StreamHandlers) => (() => void);
    addDownloadListeners: ({ onStatus, }: DownloadHandlers) => (() => void);
};
export declare const useGeminiNano: () => {
    isAvailable: () => Promise<boolean>;
    getAvailability: () => Promise<GeminiNanoAvailability>;
    availability: GeminiNanoAvailability | null;
    isCheckingAvailability: boolean;
    refreshAvailability: () => Promise<GeminiNanoAvailability>;
    downloadModel: () => Promise<void>;
    isDownloadingModel: boolean;
    downloadState: GeminiNanoDownloadEvent | null;
    generateText: (prompt: string) => Promise<string>;
    generateTextStream: (prompt: string) => void;
    stopStreaming: () => void;
    isStreaming: boolean;
    streamedText: string;
    error: GeminiNanoStreamErrorEvent | null;
};
export default GeminiNano;
