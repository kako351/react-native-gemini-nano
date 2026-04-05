import type { TurboModule } from 'react-native';
export type GeminiNanoAvailabilityStatus = 'available' | 'needs_download' | 'downloading' | 'needs_system_update' | 'busy' | 'unavailable' | 'unknown';
export type GeminiNanoAvailability = {
    status: GeminiNanoAvailabilityStatus;
    isAvailable: boolean;
    message: string;
    errorCode?: string;
};
export interface Spec extends TurboModule {
    isAvailable(): Promise<boolean>;
    getAvailability(): Promise<GeminiNanoAvailability>;
    downloadModel(): Promise<void>;
    generateText(prompt: string): Promise<string>;
    generateTextStream(prompt: string): void;
}
declare const _default: Spec | null;
export default _default;
