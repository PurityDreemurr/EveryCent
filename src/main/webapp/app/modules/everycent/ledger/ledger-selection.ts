const SELECTED_LEDGER_STORAGE_KEY = 'everycent.selectedLedgerId';
const LEGACY_SELECTED_LEDGER_STORAGE_KEY = 'everycent.aiRecord.selectedLedgerId';
const SELECTED_LEDGER_CHANGE_EVENT = 'everycent:selected-ledger-change';

const parseLedgerId = (value: string | null) => {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
};

export const readSelectedLedgerId = () => {
  try {
    return (
      parseLedgerId(window.sessionStorage.getItem(SELECTED_LEDGER_STORAGE_KEY)) ??
      parseLedgerId(window.sessionStorage.getItem(LEGACY_SELECTED_LEDGER_STORAGE_KEY))
    );
  } catch {
    return undefined;
  }
};

export const writeSelectedLedgerId = (ledgerId?: number) => {
  try {
    if (ledgerId) {
      window.sessionStorage.setItem(SELECTED_LEDGER_STORAGE_KEY, String(ledgerId));
      window.sessionStorage.removeItem(LEGACY_SELECTED_LEDGER_STORAGE_KEY);
    } else {
      window.sessionStorage.removeItem(SELECTED_LEDGER_STORAGE_KEY);
      window.sessionStorage.removeItem(LEGACY_SELECTED_LEDGER_STORAGE_KEY);
    }
  } catch {
    // Selection still works in component state when storage is unavailable.
  }

  window.dispatchEvent(new CustomEvent(SELECTED_LEDGER_CHANGE_EVENT, { detail: { ledgerId } }));
};

export const subscribeSelectedLedgerChange = (handler: (ledgerId?: number) => void) => {
  const onSelectedLedgerChange = (event: Event) => {
    handler((event as CustomEvent<{ ledgerId?: number }>).detail?.ledgerId);
  };
  const onStorageChange = (event: StorageEvent) => {
    if (event.key === SELECTED_LEDGER_STORAGE_KEY || event.key === LEGACY_SELECTED_LEDGER_STORAGE_KEY) {
      handler(readSelectedLedgerId());
    }
  };

  window.addEventListener(SELECTED_LEDGER_CHANGE_EVENT, onSelectedLedgerChange);
  window.addEventListener('storage', onStorageChange);

  return () => {
    window.removeEventListener(SELECTED_LEDGER_CHANGE_EVENT, onSelectedLedgerChange);
    window.removeEventListener('storage', onStorageChange);
  };
};
