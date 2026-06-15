import './home.scss';

import React from 'react';
import { Link } from 'react-router-dom';

import { Alert, Col, Row } from 'reactstrap';

import { useAppSelector } from 'app/config/store';

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);

  return (
    <Row>
      <Col md="3" className="pad">
        <span className="hipster rounded" />
      </Col>
      <Col md="9">
        <h1 className="display-4">欢迎, Java Hipster!</h1>
        <p className="lead">这里是首页</p>
        {account?.login ? (
          <div>
            <Alert color="success">您目前是以 &quot;{account.login}&quot; 账号登录.</Alert>
          </div>
        ) : (
          <div>
            <Alert color="warning">
              如果您要
              <span>&nbsp;</span>
              <Link to="/login" className="alert-link">
                登录
              </Link>
              , 您可以使用默认账号:
              <br />- 管理员 (账号=&quot;admin&quot;和密码=&quot;admin&quot;) <br />- 普通用户
              (账号=&quot;user&quot;和密码=&quot;user&quot;).
            </Alert>

            <Alert color="warning">
              您还没有账号&nbsp;
              <Link to="/account/register" className="alert-link">
                注册一个新账号
              </Link>
            </Alert>
          </div>
        )}
        <p>如果您有任何有关 JHipster 的问题, 可以查阅下列资源:</p>

        <ul>
          <li>
            <a href="https://www.jhipster.tech/" target="_blank" rel="noopener noreferrer">
              JHipster 首頁
            </a>
          </li>
          <li>
            <a href="https://stackoverflow.com/tags/jhipster/info" target="_blank" rel="noopener noreferrer">
              Stack Overflow 上关于 JHipster 的讨论
            </a>
          </li>
          <li>
            <a href="https://github.com/jhipster/generator-jhipster/issues?state=open" target="_blank" rel="noopener noreferrer">
              JHipster 的缺陷追踪
            </a>
          </li>
          <li>
            <a href="https://gitter.im/jhipster/generator-jhipster" target="_blank" rel="noopener noreferrer">
              JHipster public chat room
            </a>
          </li>
          <li>
            <a href="https://twitter.com/jhipster" target="_blank" rel="noopener noreferrer">
              在 Twitter 上联络 @jhipster
            </a>
          </li>
        </ul>

        <p>
          如果您喜欢 JHipster, 请记得给我们加星在{' '}
          <a href="https://github.com/jhipster/generator-jhipster" target="_blank" rel="noopener noreferrer">
            GitHub
          </a>
          !
        </p>
      </Col>
    </Row>
  );
};

export default Home;
