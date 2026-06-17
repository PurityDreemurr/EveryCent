import React from 'react';
import { NavLink } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

export type TopNavLink = {
  title: string;
  href: string;
  disabled?: boolean;
};

type TopNavProps = {
  links: TopNavLink[];
};

const TopNav = ({ links }: TopNavProps) => (
  <nav className="everycent-topnav" aria-label="Top navigation">
    <button className="everycent-icon-button everycent-topnav__menu" type="button" aria-label="Toggle navigation menu">
      <FontAwesomeIcon icon="th-list" />
    </button>
    <div className="everycent-topnav__links">
      {links.map(link => (
        <NavLink
          key={link.href}
          to={link.href}
          aria-disabled={link.disabled}
          className={({ isActive }) => `everycent-topnav__link${isActive ? ' active' : ''}${link.disabled ? ' disabled' : ''}`}
        >
          {link.title}
        </NavLink>
      ))}
    </div>
  </nav>
);

export default TopNav;
