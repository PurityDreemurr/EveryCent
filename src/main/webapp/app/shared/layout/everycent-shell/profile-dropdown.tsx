import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppSelector } from 'app/config/store';

const ProfileDropdown = () => {
  const [open, setOpen] = useState(false);
  const account = useAppSelector(state => state.authentication.account);
  const login = account?.login || 'user';
  const email = account?.email || 'user@everycent.local';

  return (
    <div className="everycent-topbar__menu-wrap">
      <button className="everycent-profile-button" type="button" aria-label="打开个人菜单" onClick={() => setOpen(value => !value)}>
        {login.slice(0, 2).toUpperCase()}
      </button>
      {open && (
        <div className="everycent-topbar__menu everycent-topbar__menu--profile">
          <div className="everycent-topbar__profile">
            <strong>{login}</strong>
            <small>{email}</small>
          </div>
          <Link to="/account/settings" onClick={() => setOpen(false)}>
            <FontAwesomeIcon icon="user" fixedWidth />
            个人资料
          </Link>
          <Link to="/account/password" onClick={() => setOpen(false)}>
            <FontAwesomeIcon icon="lock" fixedWidth />
            密码
          </Link>
          <Link className="danger" to="/logout" onClick={() => setOpen(false)}>
            <FontAwesomeIcon icon="sign-out-alt" fixedWidth />
            退出登录
          </Link>
        </div>
      )}
    </div>
  );
};

export default ProfileDropdown;
