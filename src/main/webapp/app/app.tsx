import 'react-toastify/dist/ReactToastify.css';
import './app.scss';
import 'app/config/dayjs';

import React, { useEffect } from 'react';
import { Card } from 'reactstrap';
import { BrowserRouter, useLocation } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getSession } from 'app/shared/reducers/authentication';
import { getProfile } from 'app/shared/reducers/application-profile';
import Header from 'app/shared/layout/header/header';
import Footer from 'app/shared/layout/footer/footer';
import { hasAnyAuthority } from 'app/shared/auth/private-route';
import ErrorBoundary from 'app/shared/error/error-boundary';
import { AUTHORITIES } from 'app/config/constants';
import AppRoutes from 'app/routes';

const baseHref = document.querySelector('base').getAttribute('href').replace(/\/$/, '');

const AppFrame = ({
  isAuthenticated,
  isAdmin,
  ribbonEnv,
  isInProduction,
  isOpenAPIEnabled,
}: {
  isAuthenticated: boolean;
  isAdmin: boolean;
  ribbonEnv: string;
  isInProduction: boolean;
  isOpenAPIEnabled: boolean;
}) => {
  const location = useLocation();
  const isEveryCentPage = location.pathname.startsWith('/everycent');
  const isEveryCentAccountPage = location.pathname === '/account/settings' || location.pathname === '/account/password';
  const isEveryCentLayoutPage = isEveryCentPage || isEveryCentAccountPage;
  const isAuthPage = location.pathname === '/login' || location.pathname === '/account/register';

  return (
    <div className={`app-container${isEveryCentLayoutPage ? ' app-container--everycent' : ''}${isAuthPage ? ' app-container--auth' : ''}`}>
      <ToastContainer position="top-left" className="toastify-container" toastClassName="toastify-toast" />
      {!isEveryCentLayoutPage && !isAuthPage && (
        <ErrorBoundary>
          <Header
            isAuthenticated={isAuthenticated}
            isAdmin={isAdmin}
            ribbonEnv={ribbonEnv}
            isInProduction={isInProduction}
            isOpenAPIEnabled={isOpenAPIEnabled}
          />
        </ErrorBoundary>
      )}
      <div className="container-fluid view-container" id="app-view-container">
        {isEveryCentLayoutPage || isAuthPage ? (
          <ErrorBoundary>
            <AppRoutes />
          </ErrorBoundary>
        ) : (
          <Card className="jh-card">
            <ErrorBoundary>
              <AppRoutes />
            </ErrorBoundary>
          </Card>
        )}
        {!isEveryCentLayoutPage && !isAuthPage && <Footer />}
      </div>
    </div>
  );
};

export const App = () => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(getSession());
    dispatch(getProfile());
  }, []);

  const isAuthenticated = useAppSelector(state => state.authentication.isAuthenticated);
  const isAdmin = useAppSelector(state => hasAnyAuthority(state.authentication.account.authorities, [AUTHORITIES.ADMIN]));
  const ribbonEnv = useAppSelector(state => state.applicationProfile.ribbonEnv);
  const isInProduction = useAppSelector(state => state.applicationProfile.inProduction);
  const isOpenAPIEnabled = useAppSelector(state => state.applicationProfile.isOpenAPIEnabled);

  return (
    <BrowserRouter basename={baseHref}>
      <AppFrame
        isAuthenticated={isAuthenticated}
        isAdmin={isAdmin}
        ribbonEnv={ribbonEnv}
        isInProduction={isInProduction}
        isOpenAPIEnabled={isOpenAPIEnabled}
      />
    </BrowserRouter>
  );
};

export default App;
