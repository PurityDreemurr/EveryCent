import { IconProp } from '@fortawesome/fontawesome-svg-core';

export type EveryCentUser = {
  name: string;
  email: string;
  initials: string;
};

export type EveryCentNavBase = {
  title: string;
  badge?: string;
  displayId?: string;
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
  navGroups: EveryCentNavGroup[];
};

export const everyCentSidebarData: EveryCentSidebarData = {
  user: {
    name: 'EveryCent 用户',
    email: 'user@everycent.local',
    initials: 'EC',
  },
  navGroups: [
    {
      title: '通用',
      items: [
        { title: 'AI 记账', url: '/everycent/ai', displayId: 'ai', icon: 'pencil-alt' },
        { title: '仪表盘', url: '/everycent/dashboard', displayId: 'dashboard', icon: 'tachometer-alt' },
        { title: '账本', url: '/everycent/ledgers', displayId: 'ledger', icon: 'book' },
        { title: '收支记录', url: '/everycent/transactions', displayId: 'transactions', icon: 'list' },
        { title: '预算', url: '/everycent/budgets', displayId: 'budget', icon: 'tasks' },
        { title: '成员', url: '/everycent/members', icon: 'users' },
      ],
    },
    {
      title: '洞察',
      items: [
        { title: '消息提醒', url: '/everycent/notifications', icon: 'bell' },
        { title: '导出 Excel', url: '/everycent/export', icon: 'save' },
      ],
    },
    {
      title: '其他',
      items: [
        {
          title: '标签',
          icon: 'flag',
          items: [
            { title: '行为标签', url: '/everycent/tags', icon: 'flag' },
            { title: '情绪标签', url: '/everycent/tags', icon: 'heart' },
          ],
        },
        {
          title: '设置',
          icon: 'cog',
          items: [
            { title: '个人资料', url: '/everycent/settings', icon: 'user' },
            { title: '账户设置', url: '/everycent/settings', icon: 'lock' },
          ],
        },
      ],
    },
  ],
};
