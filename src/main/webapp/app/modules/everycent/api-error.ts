import axios from 'axios';

export const getHttpStatus = (error: unknown) => (axios.isAxiosError(error) ? error.response?.status : undefined);

export const messageForLedgerError = (error: unknown, fallback: string) => {
  const status = getHttpStatus(error);

  if (status === 401) {
    return '请先登录后再访问账本。';
  }
  if (status === 403) {
    return '你没有权限访问这个账本。';
  }
  if (status === 404) {
    return '账本不存在或已被删除。';
  }

  return fallback;
};

export const messageForTransactionError = (error: unknown, fallback: string) => {
  const status = getHttpStatus(error);

  if (status === 401) {
    return '请先登录后再操作收支记录。';
  }
  if (status === 403) {
    return '你没有权限操作当前账本的收支记录。';
  }
  if (status === 404) {
    return '账本或收支记录不存在。';
  }
  if (status === 400) {
    return '请求参数有误，请确认金额为正数，并选择有效的行为标签和情绪标签。';
  }

  return fallback;
};
