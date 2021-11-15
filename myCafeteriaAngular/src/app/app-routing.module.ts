import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

//This is my case 
const routes: Routes = [];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }