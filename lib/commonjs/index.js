"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.useGeminiNano = exports.GeminiNano = exports.GEMINI_NANO_DOWNLOAD_EVENT = exports.GEMINI_NANO_STREAM_ERROR_EVENT = exports.GEMINI_NANO_STREAM_END_EVENT = exports.GEMINI_NANO_STREAM_CHUNK_EVENT = void 0;
const react_1 = require("react");
const react_native_1 = require("react-native");
const NativeGeminiNano_1 = __importDefault(require("./NativeGeminiNano"));
exports.GEMINI_NANO_STREAM_CHUNK_EVENT = 'GeminiNano:onTextChunk';
exports.GEMINI_NANO_STREAM_END_EVENT = 'GeminiNano:onTextStreamEnd';
exports.GEMINI_NANO_STREAM_ERROR_EVENT = 'GeminiNano:onTextStreamError';
exports.GEMINI_NANO_DOWNLOAD_EVENT = 'GeminiNano:onDownloadStatus';
const ensureAndroid = () => {
    if (react_native_1.Platform.OS !== 'android') {
        throw new Error('react-native-gemini-nano は Android 専用です。');
    }
};
const getNativeModule = () => {
    ensureAndroid();
    if (NativeGeminiNano_1.default == null) {
        throw new Error('GeminiNano TurboModule が見つかりません。Codegen 実行済みで、Android 側の autolinking と New Architecture が有効か確認してください。');
    }
    return NativeGeminiNano_1.default;
};
const addStreamingListeners = ({ onChunk, onComplete, onError, }) => {
    const subscriptions = [
        react_native_1.DeviceEventEmitter.addListener(exports.GEMINI_NANO_STREAM_CHUNK_EVENT, (event) => {
            onChunk?.(event.chunk);
        }),
        react_native_1.DeviceEventEmitter.addListener(exports.GEMINI_NANO_STREAM_END_EVENT, (event) => {
            onComplete?.(event.fullText ?? '');
        }),
        react_native_1.DeviceEventEmitter.addListener(exports.GEMINI_NANO_STREAM_ERROR_EVENT, (event) => {
            onError?.(event);
        }),
    ];
    return () => {
        subscriptions.forEach(subscription => subscription.remove());
    };
};
const addDownloadListeners = ({ onStatus, }) => {
    const subscription = react_native_1.DeviceEventEmitter.addListener(exports.GEMINI_NANO_DOWNLOAD_EVENT, (event) => {
        onStatus?.(event);
    });
    return () => {
        subscription.remove();
    };
};
exports.GeminiNano = {
    isAvailable() {
        return getNativeModule().isAvailable();
    },
    getAvailability() {
        return getNativeModule().getAvailability();
    },
    downloadModel(handlers = {}) {
        const removeListeners = addDownloadListeners(handlers);
        return getNativeModule()
            .downloadModel()
            .finally(() => {
            removeListeners();
        });
    },
    generateText(prompt) {
        return getNativeModule().generateText(prompt);
    },
    generateTextStream(prompt, handlers = {}) {
        const removeListeners = addStreamingListeners(handlers);
        getNativeModule().generateTextStream(prompt);
        return removeListeners;
    },
    addStreamingListeners,
    addDownloadListeners,
};
const useGeminiNano = () => {
    const [isStreaming, setIsStreaming] = (0, react_1.useState)(false);
    const [streamedText, setStreamedText] = (0, react_1.useState)('');
    const [error, setError] = (0, react_1.useState)(null);
    const [availability, setAvailability] = (0, react_1.useState)(null);
    const [isCheckingAvailability, setIsCheckingAvailability] = (0, react_1.useState)(false);
    const [isDownloadingModel, setIsDownloadingModel] = (0, react_1.useState)(false);
    const [downloadState, setDownloadState] = (0, react_1.useState)(null);
    const cleanupRef = (0, react_1.useRef)(null);
    (0, react_1.useEffect)(() => {
        return () => {
            cleanupRef.current?.();
            cleanupRef.current = null;
        };
    }, []);
    const refreshAvailability = async () => {
        setIsCheckingAvailability(true);
        try {
            const nextAvailability = await exports.GeminiNano.getAvailability();
            setAvailability(nextAvailability);
            return nextAvailability;
        }
        finally {
            setIsCheckingAvailability(false);
        }
    };
    const downloadModel = async () => {
        setIsDownloadingModel(true);
        setDownloadState(null);
        try {
            await exports.GeminiNano.downloadModel({
                onStatus: event => {
                    setDownloadState(event);
                    if (event.status === 'completed' || event.status === 'failed') {
                        setIsDownloadingModel(false);
                    }
                },
            });
            await refreshAvailability();
        }
        catch (nextError) {
            setIsDownloadingModel(false);
            throw nextError;
        }
    };
    const stopStreaming = () => {
        cleanupRef.current?.();
        cleanupRef.current = null;
        setIsStreaming(false);
    };
    const startStreaming = (prompt) => {
        stopStreaming();
        setStreamedText('');
        setError(null);
        setIsStreaming(true);
        cleanupRef.current = exports.GeminiNano.generateTextStream(prompt, {
            onChunk: chunk => {
                setStreamedText((current) => current + chunk);
            },
            onComplete: fullText => {
                setStreamedText(fullText);
                setIsStreaming(false);
                cleanupRef.current?.();
                cleanupRef.current = null;
            },
            onError: nextError => {
                setError(nextError);
                setIsStreaming(false);
                cleanupRef.current?.();
                cleanupRef.current = null;
            },
        });
    };
    return {
        isAvailable: exports.GeminiNano.isAvailable,
        getAvailability: exports.GeminiNano.getAvailability,
        availability,
        isCheckingAvailability,
        refreshAvailability,
        downloadModel,
        isDownloadingModel,
        downloadState,
        generateText: exports.GeminiNano.generateText,
        generateTextStream: startStreaming,
        stopStreaming,
        isStreaming,
        streamedText,
        error,
    };
};
exports.useGeminiNano = useGeminiNano;
exports.default = exports.GeminiNano;
