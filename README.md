# LAB211 – NHÓM 3

---

## Thành viên

| Tên thành viên | Vai trò / MSSV / Task week 1|
| ------------------- | --------------------------------------- |
| Võ Khả Nam | Leader / QE200059 / Datagenerator, CSV |
| Phạm Bửu Thịnh | Member / QE200023 / Usecase diagram, File description |
| Trần Hoàng Thịnh | Member / QE200056 / Class diagram, Schema |
| Hà Văn Sang | Member / DE201077 / Class diagram, Schema |

## Project

- E-Commerce Flash Sale Simulation


---

## Compile

Yêu cầu: Java 17+

```bash
javac -d out src/model/*.java src/repository/*.java src/controller/*.java src/view/*.java
```

## Chạy DataGenerator

```bash
java -cp src model.DataGenerator
```

## Cơ chế Lock

| Mechanism     | Mô tả                        |
| ------------- | ---------------------------- |
| NO_LOCK       |Không dùng khóa, các luồng truy cập tự do nên tốc độ nhanh nhất nhưng dễ gây xung đột và sai lệch dữ liệu.     |
| FILE_LOCK     |Khóa cấp độ tệp tin để ngăn các tiến trình khác nhau cùng chỉnh sửa một file vào cùng một thời điểm. |
| SYNCHRONIZED  |Chỉ cho phép duy nhất một luồng thực thi một đoạn mã tại một thời điểm để bảo vệ an toàn dữ liệu.  |
| OPTIMISTIC    |Không khóa khi đọc mà chỉ kiểm tra phiên bản lúc ghi, giúp tối ưu hiệu năng cho hệ thống ít xảy ra tranh chấp. |
