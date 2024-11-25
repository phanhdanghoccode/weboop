package controller;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import database.KhachHangDAO;
import model.KhachHang;

@WebServlet("/khach-hang-thay-doi-anh")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2, // 2MB
    maxFileSize = 1024 * 1024 * 10,      // 10MB
    maxRequestSize = 1024 * 1024 * 50    // 50MB
)
public class KhachHangThayDoiAnh extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public KhachHangThayDoiAnh() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Nếu không cần xử lý GET, có thể để trống hoặc chuyển hướng
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET không được hỗ trợ.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Object obj = request.getSession().getAttribute("khachHang");
        KhachHang khachHang = null;

        if (obj instanceof KhachHang) {
            khachHang = (KhachHang) obj;
        }

        if (khachHang != null) {
            try {
                // Thư mục lưu file
                String uploadPath = getServletContext().getRealPath("avatar");
                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdir(); // Tạo thư mục nếu chưa tồn tại
                }

                // Lấy file từ request
                Part filePart = request.getPart("file"); // "file" là name của input file trong form
                if (filePart != null && filePart.getSize() > 0) {
                    // Tạo tên file
                    String fileName = System.currentTimeMillis() + "_" + extractFileName(filePart);
                    String filePath = uploadPath + File.separator + fileName;

                    // Lưu file vào server
                    filePart.write(filePath);

                    // Lưu đường dẫn ảnh vào đối tượng khách hàng
                    khachHang.setDuongDanAnh(fileName);

                    // Cập nhật thông tin khách hàng trong database
                    KhachHangDAO khachHangDAO = new KhachHangDAO();
                    if (khachHangDAO.updateImage(khachHang) > 0) {
                        // Cập nhật session
                        request.getSession().setAttribute("khachHang", khachHang);
                    }
                }

                // Redirect về trang cá nhân hoặc trang khác
                response.sendRedirect("profile.jsp");

            } catch (Exception e) {
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi upload file.");
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Không tìm thấy thông tin khách hàng.");
        }
    }

    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        for (String content : contentDisp.split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf("=") + 2, content.length() - 1);
            }
        }
        return null;
    }
}
