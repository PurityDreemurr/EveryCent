import axios from 'axios';

export interface ExportTransactionsQuery {
  startDate: string;
  endDate: string;
}

export interface ExportTransactionsResult {
  blob: Blob;
  filename: string;
}

const contentDispositionFilename = (contentDisposition?: string) => {
  if (!contentDisposition) {
    return undefined;
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1].trim());
  }

  const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  return filenameMatch?.[1]?.trim();
};

export const exportLedgerTransactions = async (ledgerId: number, query: ExportTransactionsQuery): Promise<ExportTransactionsResult> => {
  const response = await axios.get<Blob>(`api/ledgers/${ledgerId}/transactions/export`, {
    params: query,
    responseType: 'blob',
  });

  return {
    blob: response.data,
    filename: contentDispositionFilename(response.headers['content-disposition']) || 'everycent-transactions.xlsx',
  };
};
