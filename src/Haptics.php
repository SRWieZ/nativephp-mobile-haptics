<?php

declare(strict_types=1);

namespace BlessedZulu\NativePHP\Mobile\Haptics;

class Haptics
{
    private const IMPACT_STYLES = ['light', 'medium', 'heavy', 'rigid', 'soft'];

    private const NOTIFICATION_TYPES = ['success', 'warning', 'error'];

    private const MIN_DURATION_MS = 1;

    private const MAX_DURATION_MS = 5000;

    private const MAX_IMPACT_REPEATS = 100;

    private const MIN_IMPACT_INTERVAL_MS = 15;

    /**
     * Trigger an impact haptic feedback, optionally repeated: `$times`
     * impacts `$intervalMs` apart, scheduled natively so they stay distinct
     * (impacts fired in the same instant merge into one).
     */
    public function impact(string $style = 'medium', int $times = 1, int $intervalMs = 50): bool
    {
        if (! in_array($style, self::IMPACT_STYLES, true)) {
            $style = 'medium';
        }

        $params = ['style' => $style];

        if ($times > 1) {
            $params['times'] = min(self::MAX_IMPACT_REPEATS, $times);
            $params['interval'] = max(self::MIN_IMPACT_INTERVAL_MS, min(self::MAX_DURATION_MS, $intervalMs));
        }

        return $this->call('Haptics.Impact', $params);
    }

    /**
     * Trigger a notification haptic feedback.
     */
    public function notification(string $type = 'success'): bool
    {
        if (! in_array($type, self::NOTIFICATION_TYPES, true)) {
            $type = 'success';
        }

        return $this->call('Haptics.Notification', ['type' => $type]);
    }

    /**
     * Trigger a selection haptic feedback (picker tick).
     */
    public function selection(): bool
    {
        return $this->call('Haptics.Selection');
    }

    /**
     * Trigger a raw vibration for the given duration in milliseconds.
     */
    public function vibrate(int $duration = 200): bool
    {
        $duration = max(self::MIN_DURATION_MS, min(self::MAX_DURATION_MS, $duration));

        return $this->call('Haptics.Vibrate', ['duration' => $duration]);
    }

    /**
     * Trigger a vibration pattern (alternating vibrate/pause durations in ms).
     */
    public function pattern(array $pattern): bool
    {
        $pattern = array_values(array_map(
            fn (int|float $ms): int => max(self::MIN_DURATION_MS, min(self::MAX_DURATION_MS, (int) $ms)),
            array_filter($pattern, fn ($v): bool => is_numeric($v)),
        ));

        if ($pattern === []) {
            return false;
        }

        return $this->call('Haptics.Pattern', ['pattern' => $pattern]);
    }

    private function call(string $function, array $params = []): bool
    {
        if (! function_exists('nativephp_call')) {
            return false;
        }

        try {
            // NativePHP core's bridge expects the params as a JSON string
            // (e.g. nativephp_call('Device.Vibrate', '{}')), not a PHP array.
            nativephp_call($function, json_encode((object) $params));

            return true;
        } catch (\Throwable) {
            return false;
        }
    }
}
