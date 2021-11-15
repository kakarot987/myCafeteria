import { Component, OnInit } from '@angular/core';
import { AuthService } from "src/app/services/auth.service";
import { GetDataService } from "src/app/services/get-data.service";
import { Order } from "src/assets/Order";

@Component({
  selector: 'app-homepage',
  templateUrl: './homepage.component.html',
  styleUrls: ['./homepage.component.css']
})
export class HomepageComponent implements OnInit {

  orderName: string;
  orderTime : string;
  orderList: Array<Order> = [];

  //variables declarations for Food Menu
  progress = 0;
  foodInfo !: any;
  retrievedImage: any;
  base64Data: any;

  //variables declaration for Login User
  errorMessage = ""
  name: any
  orgname : any
  empno : any
  mobile : any
  email : any
  id: any
  role : any = []
  username : any
  password : any  
  form: any = {};
  isLoggedIn = false
  jsonUser : any = {}
  formUser : any = {};
  today: number = Date.now();
  isUserRegistered = false
  userMessage : any

  constructor(private authService: AuthService, private getDataService: GetDataService) {
  }
  ngOnInit(): void {
    this.foodInfo = this.getDataService.getFood();
  }

  onSubmit(): void {
  this.authService.login(this.form).subscribe(
    data => {
      this.isLoggedIn = true
      console.log("admin", this.jsonUser)
      for(var index in data){
        if(data[index].roles == 'ROLE_ADMIN')
        this.role = data[index].roles;
        console.log(this.role)
        this.jsonUser = data;
        break
      }
      if(data.roles == 'ROLE_USER'){
        console.log("User data is",data)
        this.name = data.fullname;
        this.orgname = data.orgname
        this.empno = data.empno;
        this.mobile = data.mobileno;
        this.email = data.email
        this.role = data.roles;
        this.id = data.id;

      }
    },
    err => {
      this.errorMessage = err.error.message;
    }
  );
}
addOrder(oName: string , time: string){
  let orderObj = new Order();
  sessionStorage.setItem("name",oName)
  sessionStorage.setItem("time",time)
  console.log(  sessionStorage.getItem("name"), sessionStorage.getItem("time"))
  sessionStorage.getItem("name")
   orderObj.name = (sessionStorage.getItem("name") || '{}');
   orderObj.time = (sessionStorage.getItem("time") || '{}');
   this.orderList.push(orderObj);
   this.orderName ="";
   this.orderTime = "";
   console.log("order list is : ",this.orderList)
}


onSubmitUser() : void{
  this.authService.register(this.formUser).subscribe(data =>{
    this.isUserRegistered = true
    this.userMessage = data.message
    console.log(data.message)
    console.log()
  })
}

}
