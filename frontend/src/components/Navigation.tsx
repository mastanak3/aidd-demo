"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import styles from "./Navigation.module.css";

const links = [
  { href: "/", label: "ホーム" },
  { href: "/books", label: "書籍管理" },
  { href: "/members", label: "会員管理" },
  { href: "/loans", label: "貸出管理" },
];

export default function Navigation() {
  const pathname = usePathname();

  return (
    <nav className={styles.nav}>
      <div className={styles.inner}>
        <span className={styles.logo}>図書館管理システム</span>
        <ul className={styles.links}>
          {links.map((link) => (
            <li key={link.href}>
              <Link
                href={link.href}
                className={
                  pathname === link.href ||
                  (link.href !== "/" && pathname.startsWith(link.href))
                    ? styles.active
                    : styles.link
                }
              >
                {link.label}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </nav>
  );
}
