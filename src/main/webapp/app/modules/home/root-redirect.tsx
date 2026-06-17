import React from 'react';
import { Navigate } from 'react-router-dom';

import { useAppSelector } from 'app/config/store';

const RootRedirect = () => {
  const isAuthenticated = useAppSelector(state => state.authentication.isAuthenticated);
  const sessionHasBeenFetched = useAppSelector(state => state.authentication.sessionHasBeenFetched);

  if (!sessionHasBeenFetched) {
    return <div />;
  }

  return <Navigate to={isAuthenticated ? '/everycent/ai' : '/login'} replace />;
};

export default RootRedirect;
