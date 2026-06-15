import React from 'react';
import MenuItem from 'app/shared/layout/menus/menu-item';

import { NavDropdown } from './menu-components';

const accountMenuItemsAuthenticated = () => (
  <>
    <MenuItem icon="wrench" to="/account/settings" data-cy="settings">
      设置
    </MenuItem>
    <MenuItem icon="lock" to="/account/password" data-cy="passwordItem">
      密码
    </MenuItem>
    <MenuItem icon="sign-out-alt" to="/logout" data-cy="logout">
      退出
    </MenuItem>
  </>
);

const accountMenuItems = () => (
  <>
    <MenuItem id="login-item" icon="sign-in-alt" to="/login" data-cy="login">
      登录
    </MenuItem>
    <MenuItem icon="user-plus" to="/account/register" data-cy="register">
      注册
    </MenuItem>
  </>
);

export const AccountMenu = ({ isAuthenticated = false }) => (
  <NavDropdown icon="user" name="账号" id="account-menu" data-cy="accountMenu">
    {isAuthenticated && accountMenuItemsAuthenticated()}
    {!isAuthenticated && accountMenuItems()}
  </NavDropdown>
);

export default AccountMenu;
