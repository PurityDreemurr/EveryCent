import React, { useMemo, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { EveryCentNavCollapsible, EveryCentNavGroup, EveryCentNavItem } from './nav-config';

type NavGroupProps = {
  group: EveryCentNavGroup;
  collapsed: boolean;
};

const isCollapsible = (item: EveryCentNavItem): item is EveryCentNavCollapsible => 'items' in item;

const checkIsActive = (pathname: string, item: EveryCentNavItem, mainNav = false) =>
  ('url' in item && pathname === item.url) ||
  (isCollapsible(item) && item.items.some(child => pathname === child.url)) ||
  (mainNav && isCollapsible(item) && item.items.some(child => pathname.startsWith(child.url)));

const NavBadge = ({ children }: { children: React.ReactNode }) => <span className="everycent-sidebar__badge">{children}</span>;

const CollapsedSubmenu = ({ item }: { item: EveryCentNavCollapsible }) => (
  <div className="everycent-sidebar__flyout" role="menu">
    <div className="everycent-sidebar__flyout-title">
      <span>{item.title}</span>
      {item.badge && <NavBadge>{item.badge}</NavBadge>}
    </div>
    {item.items.map(subItem => (
      <NavLink
        key={`${item.title}-${subItem.title}`}
        to={subItem.url}
        className={({ isActive }) => `everycent-sidebar__flyout-link${isActive ? ' active' : ''}`}
      >
        {subItem.icon && <FontAwesomeIcon icon={subItem.icon} fixedWidth />}
        <span>{subItem.title}</span>
        {subItem.badge && <NavBadge>{subItem.badge}</NavBadge>}
      </NavLink>
    ))}
  </div>
);

const NavGroup = ({ group, collapsed }: NavGroupProps) => {
  const location = useLocation();
  const defaultOpenItems = useMemo(
    () =>
      group.items
        .filter(isCollapsible)
        .filter(item => checkIsActive(location.pathname, item, true))
        .map(item => item.title),
    [group.items, location.pathname],
  );
  const [openItems, setOpenItems] = useState<string[]>(defaultOpenItems);

  const toggleOpen = (title: string) => {
    setOpenItems(current => (current.includes(title) ? current.filter(item => item !== title) : [...current, title]));
  };

  return (
    <section className="everycent-sidebar__group">
      <h2 className="everycent-sidebar__group-title">{group.title}</h2>
      <div className="everycent-sidebar__group-items">
        {group.items.map(item => {
          if (isCollapsible(item)) {
            const isOpen = openItems.includes(item.title);
            const isActive = checkIsActive(location.pathname, item, true);

            return (
              <div key={item.title} className="everycent-sidebar__collapsible">
                <button
                  className={`everycent-sidebar__link everycent-sidebar__link--button${isActive ? ' active' : ''}`}
                  type="button"
                  onClick={() => toggleOpen(item.title)}
                  aria-expanded={isOpen}
                  title={collapsed ? item.title : undefined}
                >
                  {item.icon && <FontAwesomeIcon icon={item.icon} fixedWidth />}
                  <span>{item.title}</span>
                  {item.badge && !collapsed && <NavBadge>{item.badge}</NavBadge>}
                  {!collapsed && <FontAwesomeIcon className="everycent-sidebar__chevron" icon={isOpen ? 'times-circle' : 'plus'} />}
                </button>
                {collapsed ? (
                  <CollapsedSubmenu item={item} />
                ) : (
                  isOpen && (
                    <div className="everycent-sidebar__subnav">
                      {item.items.map(child => (
                        <NavLink
                          key={`${item.title}-${child.title}`}
                          to={child.url}
                          className={({ isActive: childIsActive }) => `everycent-sidebar__sublink${childIsActive ? ' active' : ''}`}
                        >
                          {child.icon && <FontAwesomeIcon icon={child.icon} fixedWidth />}
                          <span>{child.title}</span>
                          {child.badge && <NavBadge>{child.badge}</NavBadge>}
                        </NavLink>
                      ))}
                    </div>
                  )
                )}
              </div>
            );
          }

          return (
            <NavLink
              key={item.url}
              to={item.url}
              title={collapsed ? item.title : undefined}
              className={({ isActive }) => `everycent-sidebar__link${isActive ? ' active' : ''}`}
            >
              {item.icon && <FontAwesomeIcon icon={item.icon} fixedWidth />}
              <span>{item.title}</span>
              {item.badge && !collapsed && <NavBadge>{item.badge}</NavBadge>}
            </NavLink>
          );
        })}
      </div>
    </section>
  );
};

export default NavGroup;
