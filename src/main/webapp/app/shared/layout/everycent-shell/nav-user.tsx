import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppSelector } from 'app/config/store';
import { EveryCentUser } from './nav-config';

type NavUserProps = {
  fallbackUser: EveryCentUser;
  collapsed: boolean;
};

const NavUser = ({ fallbackUser, collapsed }: NavUserProps) => {
  const [open, setOpen] = useState(false);
  const account = useAppSelector(state => state.authentication.account);
  const user = {
    name: account?.login || fallbackUser.name,
    email: account?.email || fallbackUser.email,
    initials: (account?.login || fallbackUser.initials).slice(0, 2).toUpperCase(),
  };

  return (
    <div className="everycent-sidebar__user">
      <button
        className="everycent-sidebar__user-button"
        type="button"
        onClick={() => setOpen(value => !value)}
        title={collapsed ? user.name : undefined}
        aria-expanded={open}
      >
        <span className="everycent-sidebar__avatar">{user.initials}</span>
        <span className="everycent-sidebar__user-copy">
          <strong>{user.name}</strong>
          <small>{user.email}</small>
        </span>
        {!collapsed && <FontAwesomeIcon className="everycent-sidebar__team-caret" icon="sort" />}
      </button>
      {open && !collapsed && (
        <div className="everycent-sidebar__menu everycent-sidebar__menu--user" role="menu">
          <div className="everycent-sidebar__menu-label">Account</div>
          <Link className="everycent-sidebar__menu-link" to="/account/settings" onClick={() => setOpen(false)}>
            <FontAwesomeIcon icon="user" fixedWidth />
            <span>Profile</span>
          </Link>
          <Link className="everycent-sidebar__menu-link" to="/account/password" onClick={() => setOpen(false)}>
            <FontAwesomeIcon icon="lock" fixedWidth />
            <span>Password</span>
          </Link>
          <Link className="everycent-sidebar__menu-link everycent-sidebar__menu-link--danger" to="/logout" onClick={() => setOpen(false)}>
            <FontAwesomeIcon icon="sign-out-alt" fixedWidth />
            <span>Sign out</span>
          </Link>
        </div>
      )}
    </div>
  );
};

export default NavUser;
