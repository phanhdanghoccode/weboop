package controller;

import database.KhachHangDAO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import model.KhachHang;

@WebServlet("/khach-hang")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2, // 2MB
    maxFileSize = 1024 * 1024 * 10,      // 10MB
    maxRequestSize = 1024 * 1024 * 50   // 50MB
)
public class KhachHangController extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public KhachHangController() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String hanhDong = request.getParameter("hanhDong") != null ? request.getParameter("hanhDong") : "";
        if (hanhDong.equals("thay-doi-anh")) {
            thayDoiAnh(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Hành động không hợp lệ.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private void thayDoiAnh(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpSession session = request.getSession();
            Object obj = session.getAttribute("khachHang");
            if (obj == null) {
                response.sendRedirect("/khachhang/dangnhap.jsp");
                return;
            }

            KhachHang khachHang = (KhachHang) obj;

            // Lấy part chứa file upload
            Part filePart = request.getPart("anhDaiDien");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("baoLoi", "Bạn chưa chọn file.");
                RequestDispatcher rd = request.getRequestDispatcher("/khachhang/thaydoi.jsp");
                rd.forward(request, response);
                return;
            }

            // Đường dẫn lưu file
            String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }

            // Lưu file
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            Path filePath = Paths.get(uploadPath, fileName);
            try (InputStream fileContent = filePart.getInputStream()) {
                Files.copy(fileContent, filePath);
            }

            // Lưu tên file vào database
            KhachHangDAO khd = new KhachHangDAO();
            khachHang.setDuongDanAnh(fileName); // Cập nhật tên file ảnh
            if (khd.update(khachHang) > 0) {
                request.setAttribute("thongBao", "Thay đổi ảnh thành công!");
            } else {
                request.setAttribute("baoLoi", "Không thể lưu ảnh vào cơ sở dữ liệu!");
            }

            RequestDispatcher rd = request.getRequestDispatcher("/khachhang/thaydoi.jsp");
            rd.forward(request, response);
        } catch (IOException | ServletException e) {
            e.printStackTrace();
            request.setAttribute("baoLoi", "Có lỗi xảy ra trong quá trình upload.");
            try {
                RequestDispatcher rd = request.getRequestDispatcher("/khachhang/thaydoi.jsp");
                rd.forward(request, response);
            } catch (ServletException | IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
