import { Injectable } from '@angular/core';
const TOKEN_KEY = 'auth-token';
const USER_KEY = 'auth-user';
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  signOut(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
  }
  saveToken(token: string): void { sessionStorage.setItem(TOKEN_KEY, token); }
  getToken(): string { return sessionStorage.getItem(TOKEN_KEY) || ''; }
  saveUser(user: any): void { sessionStorage.setItem(USER_KEY, JSON.stringify(user)); }
  getUser(): any {
    try { return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null'); } catch (e) { return null; }
  }
}
