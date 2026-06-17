export type EveryCentUserFormRecord = {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  phoneNumber: string;
  role: string;
};

export type EveryCentTaskFormRecord = {
  title: string;
  status: string;
  label: string;
  priority: string;
};

export type EveryCentFormSubmit<T> = (values: T) => void;
