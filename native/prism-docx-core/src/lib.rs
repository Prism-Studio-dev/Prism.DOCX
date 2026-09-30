#![deny(unsafe_op_in_unsafe_fn)]

mod xml;

use std::panic::{UnwindSafe, catch_unwind};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i32)]
enum XmlStatus {
    Valid = 1,
    InvalidXml = 0,
    InvalidArgument = -1,
    InvalidUtf8 = -2,
    InternalError = -3,
}

/// Version of the C ABI, incremented when signatures or status semantics change.
/// This constant-only export cannot panic.
#[unsafe(no_mangle)]
pub extern "C" fn prism_core_api_version() -> u32 {
    1
}

/// Adds with the same overflow semantics as Kotlin's Int, without panicking.
#[unsafe(no_mangle)]
pub extern "C" fn prism_core_add(a: i32, b: i32) -> i32 {
    a.wrapping_add(b)
}

/// Validates a borrowed UTF-8 XML buffer. Returns an XmlStatus integer code.
/// Null with zero length represents empty XML; null with nonzero length is invalid.
///
/// # Safety
/// For nonzero length, `data` must point to one readable allocation containing
/// at least `len` initialized bytes and remain valid and unmodified until return.
/// The buffer is never written to, retained, or freed by Rust. Arbitrary dangling
/// pointers cannot be detected by this API and violate the caller's contract.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn prism_core_validate_xml_utf8(data: *const u8, len: u32) -> i32 {
    guard_xml_validation(|| {
        let len = len as usize;
        if (data.is_null() && len != 0)
            || len > isize::MAX as usize
            || (data as usize).checked_add(len).is_none()
        {
            return XmlStatus::InvalidArgument;
        }

        let bytes = if len == 0 {
            &[]
        } else {
            // SAFETY: non-null/range checks above and the caller's buffer contract.
            unsafe { std::slice::from_raw_parts(data, len) }
        };
        validate_xml_utf8(bytes)
    }) as i32
}

fn validate_xml_utf8(bytes: &[u8]) -> XmlStatus {
    match std::str::from_utf8(bytes) {
        Ok(text) if xml::validate_xml(text) => XmlStatus::Valid,
        Ok(_) => XmlStatus::InvalidXml,
        Err(_) => XmlStatus::InvalidUtf8,
    }
}

fn guard_xml_validation(validate: impl FnOnce() -> XmlStatus + UnwindSafe) -> XmlStatus {
    match catch_unwind(validate) {
        Ok(status) => status,
        Err(_) => XmlStatus::InternalError,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn reports_supported_abi_version() {
        assert_eq!(prism_core_api_version(), 1);
    }

    #[test]
    fn adds_i32_values() {
        assert_eq!(prism_core_add(2, 3), 5);
        assert_eq!(prism_core_add(-10, 4), -6);
        assert_eq!(prism_core_add(0, 0), 0);
    }

    #[test]
    fn wraps_like_kotlin_int_in_both_directions() {
        assert_eq!(prism_core_add(i32::MAX, 1), i32::MIN);
        assert_eq!(prism_core_add(i32::MIN, -1), i32::MAX);
    }

    #[test]
    fn validates_utf8_before_xml_parsing() {
        assert_eq!(validate_xml_utf8(b"<root/>"), XmlStatus::Valid);
        assert_eq!(validate_xml_utf8(b"<root>"), XmlStatus::InvalidXml);
        assert_eq!(validate_xml_utf8(b""), XmlStatus::InvalidXml);
        assert_eq!(
            validate_xml_utf8(b"<root>\xff</root>"),
            XmlStatus::InvalidUtf8
        );
        assert_eq!(validate_xml_utf8(&[0xe2, 0x82]), XmlStatus::InvalidUtf8);
    }

    #[test]
    fn ffi_respects_explicit_length_without_a_terminator() {
        let bytes = b"<root/>ignored trailing bytes";
        // SAFETY: the provided length is within the live array.
        let status = unsafe { prism_core_validate_xml_utf8(bytes.as_ptr(), 7) };
        assert_eq!(status, XmlStatus::Valid as i32);
    }

    #[test]
    fn ffi_rejects_null_nonempty_buffers_and_treats_zero_length_as_empty_xml() {
        // SAFETY: the FFI explicitly accepts null and checks it before dereferencing.
        assert_eq!(
            unsafe { prism_core_validate_xml_utf8(std::ptr::null(), 1) },
            XmlStatus::InvalidArgument as i32
        );
        // SAFETY: zero length never dereferences the pointer.
        assert_eq!(
            unsafe { prism_core_validate_xml_utf8(std::ptr::null(), 0) },
            XmlStatus::InvalidXml as i32
        );
        // SAFETY: zero length never dereferences the pointer.
        assert_eq!(
            unsafe { prism_core_validate_xml_utf8(b"unused".as_ptr(), 0) },
            XmlStatus::InvalidXml as i32
        );
    }

    #[test]
    fn ffi_reports_invalid_utf8() {
        let bytes = [0xff];
        // SAFETY: the array is initialized and alive for the call.
        assert_eq!(
            unsafe { prism_core_validate_xml_utf8(bytes.as_ptr(), bytes.len() as u32) },
            XmlStatus::InvalidUtf8 as i32
        );
    }

    #[test]
    fn ffi_guard_converts_panics_to_internal_error() {
        assert_eq!(
            guard_xml_validation(|| panic!("simulated parser panic")),
            XmlStatus::InternalError
        );
    }
}
