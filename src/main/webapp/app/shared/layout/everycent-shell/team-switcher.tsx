import React, { useRef, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { EveryCentTeam } from './nav-config';

type TeamSwitcherProps = {
  teams: EveryCentTeam[];
  collapsed: boolean;
};

const TeamSwitcher = ({ teams, collapsed }: TeamSwitcherProps) => {
  const [activeTeam, setActiveTeam] = useState(teams[0]);
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  return (
    <div className="everycent-sidebar__team" ref={containerRef}>
      <button
        className="everycent-sidebar__team-button"
        type="button"
        onClick={() => setOpen(value => !value)}
        title={collapsed ? activeTeam.name : undefined}
        aria-expanded={open}
      >
        <span className="everycent-sidebar__mark">{activeTeam.initials}</span>
        <span className="everycent-sidebar__team-copy">
          <span>Current ledger</span>
          <strong>{activeTeam.name}</strong>
          <small>{activeTeam.description}</small>
        </span>
        {!collapsed && <FontAwesomeIcon className="everycent-sidebar__team-caret" icon="sort" />}
      </button>
      {open && !collapsed && (
        <div className="everycent-sidebar__menu" role="menu">
          <div className="everycent-sidebar__menu-label">Ledgers</div>
          {teams.map((team, index) => (
            <button
              key={team.name}
              className={`everycent-sidebar__menu-item${team.name === activeTeam.name ? ' active' : ''}`}
              type="button"
              onClick={() => {
                setActiveTeam(team);
                setOpen(false);
              }}
            >
              <span className="everycent-sidebar__menu-mark">{team.initials}</span>
              <span>{team.name}</span>
              <small>{index + 1}</small>
            </button>
          ))}
          <button className="everycent-sidebar__menu-item" type="button" onClick={() => setOpen(false)}>
            <FontAwesomeIcon icon="plus" fixedWidth />
            <span>Add ledger</span>
          </button>
        </div>
      )}
    </div>
  );
};

export default TeamSwitcher;
