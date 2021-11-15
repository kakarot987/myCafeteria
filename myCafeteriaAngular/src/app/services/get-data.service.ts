import { Injectable } from '@angular/core';
import { HttpClientModule, HttpClient } from '@angular/common/http';


@Injectable({
  providedIn: 'root'
})
export class GetDataService {

  constructor(private http:HttpClient) { }
  getFood(){
    let url = 'http://localhost:8080/api/auth/getFood'
    return this.http.get(url)
  }
}
