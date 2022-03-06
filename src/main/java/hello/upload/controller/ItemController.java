package hello.upload.controller;

import hello.upload.domain.Item;
import hello.upload.domain.ItemRepository;
import hello.upload.domain.UploadFile;
import hello.upload.file.FileStore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository itemRepository;

    private final FileStore fileStore;

    @GetMapping("/items/new")
    public String newItem(ItemForm form) {
        return "item-form";
    }

    @PostMapping("/items/new")
    public String saveItem(ItemForm form, RedirectAttributes redirectAttributes) throws IOException {
        UploadFile attachFile = fileStore.storeFile(form.getAttachFile());
        List<UploadFile> storeImageFiles = fileStore.storeFiles(form.getImageFiles());

        Item item = new Item();
        item.setItemName(form.getItemName());
        item.setAttachFile(attachFile);
        item.setImageFiles(storeImageFiles);
        itemRepository.save(item);

        redirectAttributes.addAttribute("itemId",item.getId());

        return "redirect:/items/{itemId}";
    }

    @GetMapping("/items/{id}")
    public String items(@PathVariable Long id, Model model) {
        Item item = itemRepository.findById(id);
        model.addAttribute("item",item);
        return "item-view";
    }

    @ResponseBody
    @GetMapping("/images/{filename}")
    public Resource downLoadImage(@PathVariable String filename) throws MalformedURLException {
        // 이미지를 보여주기 위함
        return new UrlResource("file:" + fileStore.getFullPath(filename));
    }

    @GetMapping("/attach/{itemId}")
    public ResponseEntity<Resource> downloadAttach(@PathVariable Long itemId) throws MalformedURLException {
        Item item = itemRepository.findById(itemId);
        String storeFileName = item.getAttachFile().getStoreFileName();
        String uploadFileName = item.getAttachFile().getUploadFileName();

        UrlResource urlResource = new UrlResource("file:" + fileStore.getFullPath(storeFileName));

        log.info("uploadFileName= {}", uploadFileName);

        // 파일명이 깨지지 않도록 인코딩한다.
        String encodedUploadFileName = UriUtils.encode(uploadFileName, StandardCharsets.UTF_8);

        // 브라우저에서 첨부파일이라는 것을 인지시키기 위한 헤더
        // 이 헤더가 없으면 클릭했을때 내용이 보여진다.
        String contentDisposition = "attachment; filename=\"" + encodedUploadFileName + "\"";

        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(urlResource);
    }
}
