export interface Result<T = any> {
  code: number;
  msg: string;
  data: T;
}

export interface UserDTO {
  username: string;
  avatar: string;
  phone: string;
  motto?: string;
}

export interface LoginDTO {
  account: string;
  password: string;
}

export interface UserVO {
  id: number;
  username: string;
  email: string;
  phone: string;
  avatar: string;
  motto?: string;
}

export interface LoginVO {
  token: string;
  user: UserVO;
}

export interface CursorPageResult<T> {
  records: T[];
  nextCursor: string | null;
  hasMore: boolean;
}