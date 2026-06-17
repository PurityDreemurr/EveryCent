import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

export type EveryCentTheme = 'light' | 'dark' | 'system';
export type EveryCentResolvedTheme = Exclude<EveryCentTheme, 'system'>;

type ThemeProviderState = {
  defaultTheme: EveryCentTheme;
  resolvedTheme: EveryCentResolvedTheme;
  theme: EveryCentTheme;
  setTheme: (theme: EveryCentTheme) => void;
  resetTheme: () => void;
};

const DEFAULT_THEME: EveryCentTheme = 'system';
const THEME_COOKIE_NAME = 'everycent-theme';
const THEME_COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

const initialState: ThemeProviderState = {
  defaultTheme: DEFAULT_THEME,
  resolvedTheme: 'light',
  theme: DEFAULT_THEME,
  setTheme: () => null,
  resetTheme: () => null,
};

const ThemeContext = createContext<ThemeProviderState>(initialState);

const getCookie = (name: string) => {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
};

const setCookie = (name: string, value: string, maxAge: number) => {
  document.cookie = `${name}=${encodeURIComponent(value)}; Max-Age=${maxAge}; Path=/; SameSite=Lax`;
};

const removeCookie = (name: string) => {
  document.cookie = `${name}=; Max-Age=0; Path=/; SameSite=Lax`;
};

const getSystemTheme = (): EveryCentResolvedTheme => (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');

type ThemeProviderProps = {
  children: React.ReactNode;
  defaultTheme?: EveryCentTheme;
  storageKey?: string;
};

export const ThemeProvider = ({ children, defaultTheme = DEFAULT_THEME, storageKey = THEME_COOKIE_NAME }: ThemeProviderProps) => {
  const [theme, setThemeState] = useState<EveryCentTheme>(() => {
    const savedTheme = getCookie(storageKey);
    return savedTheme === 'light' || savedTheme === 'dark' || savedTheme === 'system' ? savedTheme : defaultTheme;
  });

  const resolvedTheme = useMemo<EveryCentResolvedTheme>(() => (theme === 'system' ? getSystemTheme() : theme), [theme]);

  useEffect(() => {
    const root = window.document.documentElement;
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const applyTheme = (nextTheme: EveryCentResolvedTheme) => {
      root.classList.remove('light', 'dark');
      root.classList.add(nextTheme);
      root.dataset.everycentTheme = nextTheme;
    };

    const handleSystemChange = () => {
      if (theme === 'system') {
        applyTheme(getSystemTheme());
      }
    };

    applyTheme(resolvedTheme);
    mediaQuery.addEventListener('change', handleSystemChange);

    return () => mediaQuery.removeEventListener('change', handleSystemChange);
  }, [resolvedTheme, theme]);

  const setTheme = (nextTheme: EveryCentTheme) => {
    setCookie(storageKey, nextTheme, THEME_COOKIE_MAX_AGE);
    setThemeState(nextTheme);
  };

  const resetTheme = () => {
    removeCookie(storageKey);
    setThemeState(defaultTheme);
  };

  const value = useMemo(
    () => ({
      defaultTheme,
      resolvedTheme,
      resetTheme,
      theme,
      setTheme,
    }),
    [defaultTheme, resolvedTheme, theme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useEveryCentTheme = () => {
  const context = useContext(ThemeContext);

  if (!context) {
    throw new Error('useEveryCentTheme must be used within <ThemeProvider>.');
  }

  return context;
};
