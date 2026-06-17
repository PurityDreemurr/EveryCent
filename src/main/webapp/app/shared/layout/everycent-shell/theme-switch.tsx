import React, { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useEveryCentTheme } from './theme-provider';

const themeOptions = [
  { value: 'light', label: 'Light' },
  { value: 'dark', label: 'Dark' },
  { value: 'system', label: 'System' },
] as const;

const ThemeSwitch = () => {
  const [open, setOpen] = useState(false);
  const { resolvedTheme, setTheme, theme } = useEveryCentTheme();

  return (
    <div className="everycent-topbar__menu-wrap">
      <button className="everycent-icon-button" type="button" aria-label="Toggle theme" onClick={() => setOpen(value => !value)}>
        <FontAwesomeIcon icon={resolvedTheme === 'dark' ? 'cloud' : 'asterisk'} />
      </button>
      {open && (
        <div className="everycent-topbar__menu">
          {themeOptions.map(option => (
            <button
              key={option.value}
              className={theme === option.value ? 'active' : ''}
              type="button"
              onClick={() => {
                setTheme(option.value);
                setOpen(false);
              }}
            >
              <span>{option.label}</span>
              {theme === option.value && <FontAwesomeIcon icon="save" />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

export default ThemeSwitch;
