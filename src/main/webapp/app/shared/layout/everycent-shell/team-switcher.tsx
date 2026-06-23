import React from 'react';

type TeamSwitcherProps = {
  collapsed: boolean;
};

const TeamSwitcher = ({ collapsed }: TeamSwitcherProps) => (
  <div className="everycent-sidebar__team">
    <div className="everycent-sidebar__team-button" title={collapsed ? 'EveryCent' : undefined}>
      <span className="everycent-sidebar__mark">
        <img src="/content/images/emotions/calm.png" alt="EveryCent" />
      </span>
      <span className="everycent-sidebar__team-copy">
        <strong>EveryCent</strong>
      </span>
    </div>
  </div>
);

export default TeamSwitcher;
