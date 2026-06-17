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
    name: 'EveryCent 用户',
    email: 'user@everycent.local',
    initials: 'EC',
  },
  teams: [
    {
      name: 'EveryCent',
      description: 'Vite + EveryCent',
      initials: 'PL',
    },
    {
      name: 'EveryCent Pro',
      description: '共享工作区',
      initials: 'FL',
    },
    {
      name: 'EveryCent Lite',
      description: '入门工作区',
      initials: 'TB',
    },
  ],
  navGroups: [
    {
      title: '通用',
      items: [
        { title: 'AI 记账', url: '/everycent/ai', icon: 'pencil-alt' },
        { title: '仪表盘', url: '/everycent/dashboard', icon: 'tachometer-alt' },
        { title: '账本', url: '/everycent/ledgers', icon: 'book' },
        { title: '收支记录', url: '/everycent/transactions', icon: 'list' },
        { title: '预算', url: '/everycent/budgets', icon: 'tasks' },
        { title: '成员', url: '/everycent/members', icon: 'users' },
      ],
    },
    {
      title: '洞察',
      items: [
        { title: '数据分析', url: '/everycent/analytics', icon: 'database' },
        { title: '消息提醒', url: '/everycent/notifications', badge: '3', icon: 'bell' },
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
        { title: '帮助中心', url: '/everycent/help', icon: 'wrench' },
      ],
    },
  ],
};
