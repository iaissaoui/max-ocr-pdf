package com.example.ocr;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class SpringRestController {

    private String originalfilename;
    private String MAXOCR_URL = "http://max-ocr-ia-sandbox.apps.ocp5.iicparis.fr.ibm.com/model/predict";

    @GetMapping("hello")
    public String hello(){

        System.out.println("Hello");

        return "Hello";


    }

    @PostMapping("pdf")
    public ResponseEntity<String> pdf(@RequestParam(name = "file") MultipartFile file) throws IOException {
        originalfilename = file.getOriginalFilename();

        System.out.println("pdf upload");
        Path targetLocation = Paths.get(originalfilename);
        file.transferTo(targetLocation);

        File dir = new File("images");
        dir.mkdir();




        PDDocument document = PDDocument.load(new File(originalfilename));
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        List<BufferedImage> pageimages = new ArrayList<>();

        ExecutorService es = Executors.newFixedThreadPool(4);

        for (int page = 0; page < document.getNumberOfPages(); ++page)
        {
            // suffix in filename will be used as the file format
            final int count = page;
            es.submit(()-> {
                try {
                    BufferedImage bim = pdfRenderer.renderImageWithDPI(count, 300, ImageType.RGB);
                    ImageIO.write(bim, "JPEG", new File(dir+"/"+count+".jpeg"));

                    System.out.println("added image " + count);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });






           //
        }

        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {

        }
        document.close();



        return ResponseEntity.ok("File Upload and conversion done!");



    }

    @PostMapping("ocr")
    public ResponseEntity<String> ocr() throws IOException {
        originalfilename = "Journal_des_Goncourt___mémoires_[...]Goncourt_Edmond_bpt6k2026664.pdf";

        File dirres = new File("jsons");
        dirres.mkdir() ;

        ExecutorService es = Executors.newFixedThreadPool(4);

        try (Stream<Path> walk = Files.walk(Paths.get("images"))) {

            List<Path> images = walk.filter(Files::isRegularFile).collect(Collectors.toList());

            for(Path p:images){
                es.submit(() ->{
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                    MultiValueMap<String, Object> map= new LinkedMultiValueMap<String, Object>();
                    map.add("image", new FileSystemResource(p));
                    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(map, headers);
                    RestTemplate restTemplate = new RestTemplate();
                    ResponseEntity<String> response = restTemplate.postForEntity( MAXOCR_URL, request , String.class );
//                    restTemplate.getMessageConverters()
//                            .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

                    Path path = Paths.get(dirres+"/"+p.getFileName()+".json");
                    try (BufferedWriter writer = Files.newBufferedWriter(path,Charset.forName("UTF-8")))
                    {
                        System.out.println(StringEscapeUtils.unescapeJava(response.getBody()));
                        writer.write(StringEscapeUtils.unescapeJava(response.getBody()));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("OCR result available for "+p.getFileName());
                } );
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        es.shutdown();
        try {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {

        }

    return ResponseEntity.ok("OCR JSON results created in directory:"+dirres);
    }


    @PostMapping("search")
    public ResponseEntity<String> search(@RequestParam String keyword) throws IOException {


        try(Stream<Path> walk = Files.walk(Paths.get("jsons")){

            List<Path> jsons = walk.filter(Files::isRegularFile).collect(Collectors.toList());

            for(Path p:jsons){



            }



        }

    }



}
