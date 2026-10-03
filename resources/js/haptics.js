/**
 * Haptic feedback bridge for NativePHP Mobile.
 *
 * Usage:
 *   import { haptics } from '@blessedzulu/nativephp-mobile-haptics';
 *   await haptics.impact('heavy');
 */

function getCsrfToken() {
    const meta = document.querySelector('meta[name="csrf-token"]');
    return meta ? meta.getAttribute('content') : '';
}

async function call(fn, params = {}) {
    const response = await fetch('/_native/api/call', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': getCsrfToken(),
        },
        // NativePHP core's /_native/api/call (BridgeCall in native.js) keys the
        // bridge method as `method`, not `function`. Upstream sent `function`,
        // which the current core endpoint ignores. Fixed in this fork.
        body: JSON.stringify({ method: fn, params }),
    });

    return response.ok;
}

export async function impact(style = 'medium', times = 1, interval = 50) {
    return call('Haptics.Impact', times > 1 ? { style, times, interval } : { style });
}

export async function notification(type = 'success') {
    return call('Haptics.Notification', { type });
}

export async function selection() {
    return call('Haptics.Selection');
}

export async function vibrate(duration = 200) {
    return call('Haptics.Vibrate', { duration });
}

export async function pattern(pat) {
    return call('Haptics.Pattern', { pattern: pat });
}

export const haptics = { impact, notification, selection, vibrate, pattern };

export default haptics;
