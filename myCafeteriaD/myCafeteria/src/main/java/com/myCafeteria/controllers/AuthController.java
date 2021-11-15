package com.myCafeteria.controllers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.MediaType;

import java.text.SimpleDateFormat;
import java.util.Date;


import com.myCafeteria.File.Entity.FileDB;
import com.myCafeteria.File.Message.ResponseFile;
import com.myCafeteria.File.Message.ResponseMessage;
import com.myCafeteria.models.ERole;
import com.myCafeteria.models.Role;
import com.myCafeteria.models.User;
import com.myCafeteria.payload.JwtResponse;
import com.myCafeteria.payload.LoginRequest;
import com.myCafeteria.payload.MessageResponse;
import com.myCafeteria.payload.SignupRequest;
import com.myCafeteria.repository.*;
import com.myCafeteria.security.UserDetailsImpl;
import com.myCafeteria.security.UserDetailsServiceImpl;
import com.myCafeteria.user_pic.PicEntity;
import com.myCafeteria.security.JwtUtils;
import com.myCafeteria.File.Service.FileStorageService;
import com.myCafeteria.foodMenu.FoodEntity;
import com.myCafeteria.foodMenu.FoodResponse;
import com.myCafeteria.foodMenu.FoodResponseMessage;
import com.myCafeteria.foodMenu.FoodService;
import com.myCafeteria.user_pic.PicRepository;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@Autowired
	private FileStorageService storageService;
	
	// Food Service
	@Autowired
	private FoodService foodService;

	@Autowired
	PicRepository picRepository;
	
	@Autowired
	private UserDetailsServiceImpl userDetailsService;
	
	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();		
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());
		List<JwtResponse> users = userDetailsService.getUser().map(user ->{
			return new JwtResponse(jwt,
					user.getId(),
					user.getUsername(),
					user.getEmail(),
					user.getFullname(),
					user.getOrgname(),
					user.getEmpno(),
					user.getMobileno(),
					user.getDate(),
					 roles);
		}).collect(Collectors.toList());
		System.out.print(roles);
		Iterator<String> iterator  = roles.iterator();
		while(iterator.hasNext()) {
		    String element = iterator.next();
			if(element == "ROLE_ADMIN") {
			System.out.print("Admin");
			return ResponseEntity.status(HttpStatus.OK).body(users);
			}
			else if(element == "ROLE_USER")
			{
				return ResponseEntity.ok(new JwtResponse(jwt, 
						 userDetails.getId(), 
						 userDetails.getUsername(), 
						 userDetails.getEmail(), 
						 userDetails.getFullname(),
						 userDetails.getOrgname(),
						 userDetails.getEmpno(),
						 userDetails.getMobileno(),
						 userDetails.getDate(),
						 roles));	
			}
			}
		return null;
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Username is already taken!"));
		}

		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Email is already in use!"));
		}

		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
		System.out.println(formatter.format(date));
		System.out.println(date);
		// Create new user's account
		User user = new User(signUpRequest.getUsername(), 
							 signUpRequest.getEmail(),
							 encoder.encode(signUpRequest.getPassword()), 
							 signUpRequest.getFullname(),
							 signUpRequest.getOrgname(),
							 signUpRequest.getEmpno(),
							 signUpRequest.getMobileno(),
							 date);

		Set<String> strRoles = signUpRequest.getRole();
		Set<Role> roles = new HashSet<>();

		if (strRoles == null) {
			Role userRole = roleRepository.findByName(ERole.ROLE_USER)
					.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
			roles.add(userRole);
		} else {
			strRoles.forEach(role -> {
				switch (role) {
				case "admin":
					Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(adminRole);

					break;
				case "mod":
					Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(modRole);

					break;
				default:
					Role userRole = roleRepository.findByName(ERole.ROLE_USER)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(userRole);
				}
			});
		}

		user.setRoles(roles);
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("User registered successfully!" + "Registration Id is" + user.getId() + "Registration Date is " + user.getDate()));
	}
	
	  @PostMapping("/upload")
	  public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file) {
	    String message = "";
	    try {
	      storageService.store(file);

	      message = "Uploaded the file successfully: " + file.getOriginalFilename();
	      return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
	    } catch (Exception e) {
	      message = "Could not upload the file: " + file.getOriginalFilename() + "!";
	      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
	    }
	  }
	  
	  // post method for food
	  @PostMapping(value = "/uploadfood", consumes = {
			  MediaType.APPLICATION_JSON_VALUE,
			  MediaType.MULTIPART_FORM_DATA_VALUE
	  })
	  public ResponseEntity<FoodResponseMessage> uploadFood(@RequestPart("file") MultipartFile file, @RequestParam("foodBody") String foodBody){
		  String message = "";
		  try {
			  System.out.println("food Data here " + foodBody);
			  foodService.store(file, foodBody);
			  message = "Uploaded the details seccessfully: ";
			  return ResponseEntity.status(HttpStatus.OK).body(new FoodResponseMessage(message));
		  }
		  catch(Exception e) {
			  message = "Could not upload the file";
			  return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new FoodResponseMessage(message));
		  }
	  }

	  // get method for food
	  @GetMapping("/getFood")
	  public ResponseEntity<List<FoodResponse>> getfood(){
		  List<FoodResponse> food = foodService.getAllFiles().map(foodEntity ->{
			  	String fileDownloadUri = ServletUriComponentsBuilder
			  			.fromCurrentContextPath()
			  			.path("/getFood/")
			  			.path(foodEntity.getId())
			  			.toUriString();
			  	
			  	return new FoodResponse(
			  			foodEntity.getName(),
			  			fileDownloadUri,
			  			foodEntity.getType(),
			  			foodEntity.getData().length,
			  			foodEntity.getFoodname(),
			  			foodEntity.getTime(),
			  			foodEntity.getDescription(),
			  			foodEntity.getCategory(),
			  			foodEntity.getRates(),
			  			foodEntity.getId());
		  }).collect(Collectors.toList());
		  
		  return ResponseEntity.status(HttpStatus.OK).body(food);
	  }
	  
	  @GetMapping("/files")
	  public ResponseEntity<List<ResponseFile>> getListFiles() {
	    List<ResponseFile> files = storageService.getAllFiles().map(dbFile -> {
	      String fileDownloadUri = ServletUriComponentsBuilder
	          .fromCurrentContextPath()
	          .path("/files/")
	          .path(dbFile.getId())
	          .toUriString();

	      return new ResponseFile(
	          dbFile.getName(),
	          fileDownloadUri,
	          dbFile.getType(),
	          dbFile.getData().length,
	          dbFile.getId());
	    }).collect(Collectors.toList());
	    return ResponseEntity.status(HttpStatus.OK).body(files);
	  }
	  
	  //Get food image data by id
	  @GetMapping("/getFood/{id}")
	  public ResponseEntity<byte[]> getFood(@PathVariable String id) {
		  FoodEntity foodEntity = foodService.getFood(id);
		  
		  return ResponseEntity.ok()
				  .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + foodEntity.getName() + "\"")
				  .body(foodEntity.getData());
	  }

	  @GetMapping("/files/{id}")
	  public ResponseEntity<byte[]> getFile(@PathVariable String id) {
	    FileDB fileDB = storageService.getFile(id);

	    return ResponseEntity.ok()
	        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDB.getName() + "\"")
	        .body(fileDB.getData());
	  }
	  
	  @PostMapping("/userpic")
	  public @ResponseBody PicEntity postPic(@RequestParam ("picid") String picId, @RequestParam ("userId") Long userid){
		  PicEntity picEntity = new PicEntity();
		  picEntity.setId(userid);
		  picEntity.setPic_id(picId);
		  picRepository.save(picEntity);
		  return new PicEntity();
	  }
	  
	  @GetMapping("/getuserpic")
	  public @ResponseBody Iterable<PicEntity> getPic(){
		  return picRepository.findAll();
	  }
	  
}
