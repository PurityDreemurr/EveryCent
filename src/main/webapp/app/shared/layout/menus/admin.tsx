import React from 'react';
import MenuItem from 'app/shared/layout/menus/menu-item';

import { NavDropdown } from './menu-components';

const adminMenuItems = () => (
  <>
    <MenuItem icon="users" to="/admin/user-management">
      用户管理
    </MenuItem>
    <MenuItem icon="tachometer-alt" to="/admin/metrics">
      资源监控
    </MenuItem>
    <MenuItem icon="heart" to="/admin/health">
      服务状态
    </MenuItem>
    <MenuItem icon="cogs" to="/admin/configuration">
      配置
    </MenuItem>
    <MenuItem icon="tasks" to="/admin/logs">
      日志
    </MenuItem>
    {/* jhipster-needle-add-element-to-admin-menu - JHipster will add entities to the admin menu here */}
  </>
);

const openAPIItem = () => (
  <MenuItem icon="book" to="/admin/docs">
    API
  </MenuItem>
);

export const AdminMenu = ({ showOpenAPI }) => (
  <NavDropdown icon="users-cog" name="管理" id="admin-menu" data-cy="adminMenu">
    {adminMenuItems()}
    {showOpenAPI && openAPIItem()}
  </NavDropdown>
);

export default AdminMenu;
