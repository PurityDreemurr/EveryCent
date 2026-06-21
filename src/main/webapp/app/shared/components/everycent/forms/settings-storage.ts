type SettingsScope = 'profile' | 'account' | 'appearance' | 'display' | 'notifications';

export const settingsChangedEvent = 'everycent:settings-changed';

const storageKey = (scope: SettingsScope) => `everycent.settings.${scope}`;

export const loadSettings = <T>(scope: SettingsScope, fallback: T): T => {
  if (typeof window === 'undefined') {
    return fallback;
  }

  const rawValue = window.localStorage.getItem(storageKey(scope));
  if (!rawValue) {
    return fallback;
  }

  try {
    return { ...fallback, ...JSON.parse(rawValue) };
  } catch {
    return fallback;
  }
};

export const saveSettings = <T>(scope: SettingsScope, values: T) => {
  window.localStorage.setItem(storageKey(scope), JSON.stringify(values));
  window.dispatchEvent(new CustomEvent(settingsChangedEvent, { detail: { scope } }));
};
