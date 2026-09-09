import NativeHCEModule, {HCEModuleBackgroundEvent} from "./NativeHCEModule";

/**
 * Android: Data passed by the native side to the 'handleBackgroundHCECall' headless task.
 * Pass it as-is to createBackgroundHCE().
 */
export interface BackgroundHCETaskData {
    /**
     * Handle of the background HCE session.
     */
    handle: string,
}

export interface BackgroundEventHandler {
    (
        event: HCEModuleBackgroundEvent,
        respondAPDU: (rapdu: string) => Promise<void>,
        taskData: BackgroundHCETaskData,
    ): Promise<void>
}

export interface ProcessBackgroundHCEFunc {
    (
        handler: BackgroundEventHandler
    ): void
}

/**
 * Set up handling of a background HCE session.
 *
 * @param taskData Data received by the 'handleBackgroundHCECall' headless task. A bare handle
 *                 string is accepted as well, for backwards compatibility.
 */
export const createBackgroundHCE = (taskData: BackgroundHCETaskData | string): ProcessBackgroundHCEFunc => {
    const useTaskData: BackgroundHCETaskData = typeof taskData === "string"
        ? {handle: taskData}
        : taskData;
    const handle = useTaskData.handle;

    const respondAPDU = (rapdu: string) => {
        return NativeHCEModule.respondAPDU(handle, rapdu);
    }

    return (handler: BackgroundEventHandler) => {
        const subscription = NativeHCEModule.onBackgroundEvent(async (event) => {
            if (event.audience !== handle) {
                // ignore
                return;
            }

            try {
                await handler(event, respondAPDU, useTaskData);
            } catch (e) {
                throw e;
            } finally {
                if (event.type === "readerDeselected") {
                    subscription.remove();
                    NativeHCEModule.finishBackgroundHCE(handle);
                }
            }
        });

        if (!NativeHCEModule.beginBackgroundHCE(handle)) {
            subscription.remove();
            NativeHCEModule.finishBackgroundHCE(handle);
        }
    }
}
