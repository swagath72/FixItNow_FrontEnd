package com.simats.fixitnow.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*

// Chat Models
data class ChatMessage(
    val id: Int? = null,
    @SerializedName("sender_email") val senderEmail: String,
    @SerializedName("receiver_email") val receiverEmail: String,
    val message: String,
    val timestamp: String? = null,
    @SerializedName("is_sent_by_me") val isSentByMe: Boolean = false,
    val status: String? = "sent"
)

data class SendMessageRequest(
    @SerializedName("receiver_email") val receiverEmail: String,
    val message: String
)

data class ChatListItem(
    val name: String,
    val email: String,
    @SerializedName("last_message") val lastMessage: String,
    val time: String,
    @SerializedName("unread_count") val unreadCount: Int,
    val role: String,
    @SerializedName("profile_pic_url") val profilePicUrl: String? = null
)

data class ChatResponse(
    val messages: List<ChatMessage>,
    @SerializedName("is_active") val isActive: Boolean
)

data class UploadResponse(
    val status: String,
    val url: String
)

data class AiChatRequest(
    val message: String
)

data class AiChatResponse(
    val response: String
)

// Auth Models
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(
    val status: String,
    val message: String,
    val token: String?,
    val role: String?,
    @SerializedName("full_name") val fullName: String?,
    val phone: String?,
    @SerializedName("booked_technician_name") val bookedTechnicianName: String?,
    @SerializedName("booking_status") val bookingStatus: String?,
    @SerializedName("house_number") val houseNumber: String?,
    val street: String?,
    val area: String?,
    val city: String?,
    val state: String?,
    val pincode: String?,
    val landmark: String?,
    @SerializedName("has_completed_onboarding") val hasCompletedOnboarding: Boolean,
    @SerializedName("profile_pic_url") val profilePicUrl: String?,
    @SerializedName("verification_status") val verificationStatus: String?
)

data class RegisterRequest(
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val password: String,
    val phone: String
)
data class RegisterResponse(val message: String)

data class RoleSelectionRequest(val role: String)
data class RoleSelectionResponse(val message: String)

data class ForgotPasswordRequest(val email: String)
data class ForgotPasswordResponse(val message: String)

data class VerifyOtpRequest(val email: String, val otp: String)
data class VerifyOtpResponse(val message: String)

data class ResetPasswordRequest(
    val email: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("confirm_password") val confirmPassword: String
)
data class ResetPasswordResponse(val message: String)

// Address Models
data class AddAddressRequest(
    @SerializedName("house_number") val houseNumber: String,
    val street: String,
    val area: String,
    val city: String,
    val state: String,
    val pincode: String,
    val landmark: String?
)
data class AddAddressResponse(val message: String)

// Technician Models
data class TechnicianResponse(
    val id: Int,
    @SerializedName("full_name") val fullName: String?,
    val email: String?,
    val role: String?,
    val experience: String?,
    val skills: String?,
    val distance: String? = null
)

data class TechnicianEarningsResponse(
    @SerializedName("today_earnings") val today: String,
    @SerializedName("week_earnings") val week: String,
    @SerializedName("month_earnings") val month: String
)

data class UserProfileResponse(
    @SerializedName("full_name") val fullName: String?,
    val email: String?,
    val phone: String?,
    @SerializedName("house_number") val houseNumber: String?,
    val street: String?,
    val area: String?,
    val city: String?,
    val state: String?,
    val pincode: String?,
    val landmark: String?
)

data class CustomerProfileResponse(
    @SerializedName("full_name") val fullName: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    @SerializedName("wallet_balance") val walletBalance: String?,
    val language: String?,
    @SerializedName("profile_pic_url") val profilePicUrl: String?
)

data class TechnicianProfileResponse(
    @SerializedName("full_name") val fullName: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val skills: String?,
    @SerializedName("service_radius") val serviceRadius: String?,
    @SerializedName("working_hours") val workingHours: String?,
    @SerializedName("verification_status") val verificationStatus: String?,
    @SerializedName("payout_settings") val payoutSettings: String?,
    @SerializedName("is_online") val isOnline: String?,
    val rating: String?,
    @SerializedName("profile_pic_url") val profilePicUrl: String?
)

data class UpdateProfileRequest(
    @SerializedName("full_name") val fullName: String,
    val phone: String,
    val email: String
)

// Booking Models
data class BookingResponse(
    val id: Int?,
    val address: String?,
    val date: String?,
    val time: String?,
    val description: String?,
    @SerializedName("service_name") val serviceName: String?,
    @SerializedName("technician_name") val technicianName: String?,
    @SerializedName("technician_email") val technicianEmail: String?,
    @SerializedName("customer_name") val customerName: String?,
    @SerializedName("customer_email") val customerEmail: String?,
    val status: String?,
    @SerializedName("payment_status") val paymentStatus: String?,
    val cost: String?,
    @SerializedName("work_photo_url") val workPhotoUrl: String?,
    @SerializedName("rating_value") val ratingValue: Int?,
    @SerializedName("rating_comment") val ratingComment: String?
)

data class CreateBookingRequest(
    @SerializedName("customer_email") val customerEmail: String,
    @SerializedName("technician_email") val technicianEmail: String,
    @SerializedName("technician_name") val technicianName: String,
    @SerializedName("address") val address: String,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("description") val description: String,
    @SerializedName("cost") val cost: String,
    @SerializedName("service_name") val serviceName: String,
    @SerializedName("technician_id") val technicianId: Int,
    @SerializedName("customer_name") val customerName: String
)
data class CreateBookingResponse(val message: String, val booking_id: Int)

data class FavoriteTechnicianResponse(
    val id: Int,
    @SerializedName("full_name") val fullName: String?,
    val email: String?,
    val role: String?,
    val rating: String?,
    @SerializedName("profile_pic_url") val profilePicUrl: String?,
    val experience: String?,
    val distance: String?
)
 
data class SubmitRatingRequest(
    @SerializedName("booking_id") val bookingId: Int,
    val rating: Float,
    val comment: String
)
 
data class UpdateJobStatusRequest(
    @SerializedName("booking_id") val bookingId: Int,
    val status: String
)

data class ReviewResponse(
    val id: Int,
    @SerializedName("technician_name") val technicianName: String?,
    @SerializedName("service_name") val serviceName: String?,
    val rating: Float?,
    val comment: String?,
    val date: String?,
    @SerializedName("helpful_count") val helpfulCount: Int?
)

data class TechnicianOnboardingRequest(
    val skills: String,
    val experience: String
)

data class UpdateStatusRequest(
    @SerializedName("is_online") val isOnline: Boolean
)

data class RazorpayOrderRequest(
    @SerializedName("booking_id") val bookingId: Int,
    val amount: Double
)

data class RazorpayOrderResponse(
    @SerializedName("order_id") val orderId: String,
    val amount: Int,
    val currency: String,
    @SerializedName("key_id") val keyId: String
)

data class PaymentVerificationRequest(
    @SerializedName("booking_id") val bookingId: Int,
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerializedName("razorpay_order_id") val razorpayOrderId: String,
    @SerializedName("razorpay_signature") val razorpaySignature: String
)

data class UpdateLocationRequest(val latitude: Double, val longitude: Double)
data class LocationResponse(val latitude: String?, val longitude: String?)

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("select-role")
    fun selectRole(@Header("Authorization") token: String, @Body request: RoleSelectionRequest): Call<RoleSelectionResponse>

    @POST("forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<ForgotPasswordResponse>

    @POST("verify-otp")
    fun verifyOtp(@Body request: VerifyOtpRequest): Call<VerifyOtpResponse>

    @POST("reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<ResetPasswordResponse>

    @POST("add-address")
    fun addAddress(@Header("Authorization") token: String, @Body request: AddAddressRequest): Call<AddAddressResponse>

    @GET("technicians")
    fun getTechnicians(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): Call<List<TechnicianResponse>>

    @POST("create-booking")
    fun createBooking(@Header("Authorization") token: String, @Body request: CreateBookingRequest): Call<CreateBookingResponse>

    @GET("active-bookings")
    fun getActiveBookings(@Header("Authorization") token: String): Call<List<BookingResponse>>

    @GET("recent-bookings")
    fun getRecentBookings(@Header("Authorization") token: String, @Query("date") date: String): Call<List<BookingResponse>>

    @GET("booking-history")
    fun getBookingHistory(@Header("Authorization") token: String): Call<List<BookingResponse>>

    // Technician Specific
    @GET("technician/earnings")
    fun getTechnicianEarnings(@Header("Authorization") token: String): Call<TechnicianEarningsResponse>

    @GET("technician/jobs")
    fun getTechnicianJobs(@Header("Authorization") token: String): Call<List<BookingResponse>>

    @GET("technician/active-jobs")
    fun getTechnicianActiveJobs(@Header("Authorization") token: String): Call<List<BookingResponse>>

    @GET("technician/booking-history")
    fun getTechnicianHistory(@Header("Authorization") token: String): Call<List<BookingResponse>>

    @GET("user/profile")
    fun getUserProfile(@Header("Authorization") token: String): Call<UserProfileResponse>

    @GET("user/customer-profile")
    fun getCustomerProfile(@Header("Authorization") token: String): Call<CustomerProfileResponse>

    @GET("user/technician-profile")
    fun getTechnicianProfile(@Header("Authorization") token: String): Call<TechnicianProfileResponse>

    @POST("user/update-profile")
    fun updateProfile(@Header("Authorization") token: String, @Body request: UpdateProfileRequest): Call<Void>

    @POST("technician/update-job-status")
    fun updateJobStatus(@Header("Authorization") token: String, @Body request: UpdateJobStatusRequest): Call<Void>
 
    @POST("submit-rating")
    fun submitRating(@Header("Authorization") token: String, @Body request: SubmitRatingRequest): Call<Void>
 
    @POST("technician/update-onboarding")
    fun updateTechnicianOnboarding(@Header("Authorization") token: String, @Body request: TechnicianOnboardingRequest): Call<Void>

    @POST("technician/update-availability")
    fun updateTechnicianStatus(@Header("Authorization") token: String, @Body request: UpdateStatusRequest): Call<Void>
 
    // Chat Endpoints
    @GET("chat/messages/{other_email}")
    fun getMessages(@Header("Authorization") token: String, @Path("other_email") otherEmail: String): Call<ChatResponse>

    @POST("chat/send")
    fun sendMessage(@Header("Authorization") token: String, @Body request: SendMessageRequest): Call<ChatMessage>

    @GET("chat/list")
    fun getChatList(@Header("Authorization") token: String): Call<List<ChatListItem>>
 
    @POST("ai-chat")
    fun sendAiChat(@Body request: AiChatRequest): Call<AiChatResponse>

    @GET("favorites")
    fun getFavorites(@Header("Authorization") token: String): Call<List<FavoriteTechnicianResponse>>
 
    @GET("my-reviews")
    fun getMyReviews(@Header("Authorization") token: String): Call<List<ReviewResponse>>
 
    @Multipart
    @POST("upload-profile-pic")
    fun uploadProfilePic(
        @Header("Authorization") token: String,
        @Part file: okhttp3.MultipartBody.Part
    ): Call<UploadResponse>

    @Multipart
    @POST("upload-work-photo/{booking_id}")
    fun uploadWorkPhoto(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int,
        @Part file: okhttp3.MultipartBody.Part
    ): Call<UploadResponse>

    @Multipart
    @POST("upload-technician-document")
    fun uploadTechnicianDocument(
        @Header("Authorization") token: String,
        @Query("doc_type") docType: String,
        @Part file: okhttp3.MultipartBody.Part
    ): Call<UploadResponse>

    @POST("create-razorpay-order")
    fun createRazorpayOrder(@Header("Authorization") token: String, @Body request: RazorpayOrderRequest): Call<RazorpayOrderResponse>

    @POST("verify-payment")
    fun verifyPayment(@Header("Authorization") token: String, @Body request: PaymentVerificationRequest): Call<Void>

    @POST("mock-pay/{booking_id}")
    fun mockPay(@Header("Authorization") token: String, @Path("booking_id") bookingId: Int): Call<Void>

    @POST("update-location")
    fun updateLocation(@Header("Authorization") token: String, @Body request: UpdateLocationRequest): Call<Void>

    @GET("get-technician-location/{email}")
    fun getTechnicianLocation(@Path("email") email: String): Call<LocationResponse>

    @GET("get-user-phone/{email}")
    fun getUserPhone(@Header("Authorization") token: String, @Path("email") email: String): Call<UserPhoneResponse>

    // Admin Endpoints
    @GET("admin/technicians/pending")
    fun getPendingTechnicians(@Header("Authorization") token: String): Call<List<PendingTechnician>>

    @GET("admin/technicians/approved")
    fun getApprovedTechnicians(@Header("Authorization") token: String): Call<List<PendingTechnician>>

    @GET("admin/technicians/{user_id}/documents")
    fun getTechnicianDocuments(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): Call<List<TechnicianDocument>>

    @POST("admin/technicians/{user_id}/verify")
    fun verifyTechnician(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int,
        @Body request: VerifyRequest
    ): Call<VerifyResponse>
}

data class PendingTechnician(
    val id: Int,
    val full_name: String,
    val email: String,
    val phone: String,
    val skills: String?,
    val experience: String?,
    @SerializedName("profile_pic_url") val profile_pic_url: String?,
    @SerializedName("verification_status") val verification_status: String
)

data class TechnicianDocument(
    val id: Int,
    @SerializedName("doc_type") val doc_type: String,
    @SerializedName("file_url") val file_url: String,
    @SerializedName("uploaded_at") val uploaded_at: String
)

data class VerifyRequest(
    val status: String,
    val remarks: String? = null
)

data class VerifyResponse(val message: String)

data class UserPhoneResponse(val phone: String)
