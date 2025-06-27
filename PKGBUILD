pkgname=WebDAV2UPnP
pkgver=1.0.0
pkgrel=1
pkgdesc="WebDAV to UPnP proxy on 57416 port"
arch=('x86_64')
url="https://github.com/maxzxwd/WebDAV2UPnP"
license=('MIT')
depends=()
source=(
    "https://github.com/maxzxwd/WebDAV2UPnP/releases/download/${pkgver}/WebDAV2UPnP.service"
    "https://github.com/maxzxwd/WebDAV2UPnP/releases/download/${pkgver}/WebDAV2UPnP"
)
sha256sums=(
    'SKIP'
    'SKIP'
)
options=(!strip)

package() {
    install -Dm755 "$srcdir/WebDAV2UPnP" "$pkgdir/usr/bin/WebDAV2UPnP"
    install -Dm644 "$srcdir/WebDAV2UPnP.service" "$pkgdir/usr/lib/systemd/system/WebDAV2UPnP.service"
}