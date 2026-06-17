import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { everyCentSidebarData } from './nav-config';
import type { EveryCentTheme } from './theme-provider';
import { useEveryCentTheme } from './theme-provider';

type CommandMenuProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

const themeOptions: { icon: 'asterisk' | 'cloud' | 'cogs'; label: string; value: EveryCentTheme }[] = [
  { value: 'light', label: 'Light', icon: 'asterisk' },
  { value: 'dark', label: 'Dark', icon: 'cloud' },
  { value: 'system', label: 'System', icon: 'cogs' },
];

const getCommandItems = () =>
  everyCentSidebarData.navGroups.flatMap(group =>
    group.items.flatMap(item => {
      if ('url' in item) {
        return [{ group: group.title, title: item.title, url: item.url }];
      }

      return item.items.map(subItem => ({
        group: group.title,
        title: `${item.title} / ${subItem.title}`,
        url: subItem.url,
      }));
    }),
  );

const CommandMenu = ({ open, onOpenChange }: CommandMenuProps) => {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const { setTheme } = useEveryCentTheme();
  const commandItems = useMemo(getCommandItems, []);
  const filteredItems = commandItems.filter(item => item.title.toLowerCase().includes(query.toLowerCase()));
  const hasNoResults = filteredItems.length === 0;

  if (!open) {
    return null;
  }

  const runCommand = (command: () => void) => {
    onOpenChange(false);
    command();
  };

  const closeCommandMenu = () => onOpenChange(false);
  const updateQuery = (event: React.ChangeEvent<HTMLInputElement>) => setQuery(event.target.value);

  return (
    <div className="everycent-command" role="dialog" aria-modal="true" aria-label="Command menu">
      <button className="everycent-command__backdrop" type="button" aria-label="Close search" onClick={closeCommandMenu} />
      <div className="everycent-command__panel">
        <div className="everycent-command__input-wrap">
          <FontAwesomeIcon icon="search" />
          <input value={query} onChange={updateQuery} autoFocus placeholder="Type a command or search..." />
        </div>
        <div className="everycent-command__list">
          <div className="everycent-command__group-title">Navigation</div>
          {hasNoResults ? <div className="everycent-command__empty">No results found.</div> : null}
          {filteredItems.map(item => (
            <button key={item.group + '-' + item.title + '-' + item.url} type="button" onClick={() => runCommand(() => navigate(item.url))}>
              <FontAwesomeIcon icon="arrow-left" rotation={180} fixedWidth />
              <span>{item.title}</span>
              <small>{item.group}</small>
            </button>
          ))}
          <div className="everycent-command__group-title">Theme</div>
          {themeOptions.map(option => (
            <button key={option.value} type="button" onClick={() => runCommand(() => setTheme(option.value))}>
              <FontAwesomeIcon icon={option.icon} fixedWidth />
              <span>{option.label}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default CommandMenu;
