import { IconProp } from '@fortawesome/fontawesome-svg-core';

export type EveryCentUser = {
  name: string;
  email: string;
  initials: string;
};

export type EveryCentTeam = {
  name: string;
  description: string;
  initials: string;
};

export type EveryCentNavBase = {
  title: string;
  badge?: string;
  icon?: IconProp;
};

export type EveryCentNavLink = EveryCentNavBase & {
  url: string;
  items?: never;
};

export type EveryCentNavCollapsible = EveryCentNavBase & {
  items: EveryCentNavLink[];
  url?: never;
};

export type EveryCentNavItem = EveryCentNavLink | EveryCentNavCollapsible;

export type EveryCentNavGroup = {
  title: string;
  items: EveryCentNavItem[];
};

export type EveryCentSidebarData = {
  user: EveryCentUser;
  teams: EveryCentTeam[];
  navGroups: EveryCentNavGroup[];
};

export const everyCentSidebarData: EveryCentSidebarData = {
  user: {
    name: 'EveryCent User',
    email: 'user@everycent.local',
    initials: 'EC',
  },
  teams: [
    {
      name: 'Personal Ledger',
      description: 'Default ledger',
      initials: 'PL',
    },
    {
      name: 'Family Ledger',
      description: 'Shared ledger',
      initials: 'FL',
    },
    {
      name: 'Travel Budget',
      description: 'Budget planning',
      initials: 'TB',
    },
  ],
  navGroups: [
    {
      title: 'General',
      items: [
        { title: 'AI Record', url: '/everycent/ai', icon: 'pencil-alt' },
        { title: 'Dashboard', url: '/everycent/dashboard', icon: 'tachometer-alt' },
        { title: 'Ledgers', url: '/everycent/ledgers', icon: 'book' },
        { title: 'Transactions', url: '/everycent/transactions', icon: 'list' },
        { title: 'Budgets', url: '/everycent/budgets', icon: 'tasks' },
        { title: 'Members', url: '/everycent/members', icon: 'users' },
      ],
    },
    {
      title: 'Insights',
      items: [
        { title: 'Analytics', url: '/everycent/analytics', icon: 'database' },
        { title: 'Notifications', url: '/everycent/notifications', badge: '3', icon: 'bell' },
        { title: 'Export Excel', url: '/everycent/export', icon: 'save' },
      ],
    },
    {
      title: 'Other',
      items: [
        {
          title: 'Tags',
          icon: 'flag',
          items: [
            { title: 'Behavior Tags', url: '/everycent/tags', icon: 'flag' },
            { title: 'Emotion Tags', url: '/everycent/tags', icon: 'heart' },
          ],
        },
        {
          title: 'Settings',
          icon: 'user',
          items: [
            { title: 'Profile', url: '/everycent/settings', icon: 'user' },
            { title: 'Account', url: '/everycent/settings', icon: 'lock' },
          ],
        },
        { title: 'Help Center', url: '/everycent/help', icon: 'wrench' },
      ],
    },
  ],
};
