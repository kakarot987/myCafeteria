package com.myCafeteria.foodMenu;

import java.io.IOException;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FoodService {
	
	@Autowired
	FoodRepository foodRepository;
	
	public FoodEntity store(MultipartFile file, String foodBody) throws IOException{
		 String fileName = StringUtils.cleanPath(file.getOriginalFilename());
		 		 
		 ObjectMapper objectMapper = new ObjectMapper();
		 // Invalid JSON must fail the request; never create a partially populated food record.
		 FoodBody json = objectMapper.readValue(foodBody, FoodBody.class);
		 FoodEntity entityf = new FoodEntity(json.getFoodname(), json.getRates(), json.getTime(),json.getDescription(),json.getCategory() ,fileName
				,file.getContentType() ,file.getBytes());
		 return foodRepository.save(entityf);
	}
	
	public Stream<FoodEntity> getAllFiles(){
		return foodRepository.findAll().stream();
	}
	
	public FoodEntity getFood(String id) {
		return foodRepository.findById(id).orElse(null);
	}
	
	/*
	 *   public FileDB getFile(String id) {
    return fileDBRepository.findById(id).get();
  }
	 */
}
