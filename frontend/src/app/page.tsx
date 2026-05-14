import Link from "next/link";
import styles from "./page.module.css";

const sections = [
  { href: "/books", title: "書籍管理", desc: "書籍の登録・編集・削除・一覧表示" },
  { href: "/members", title: "会員管理", desc: "会員の登録・編集・削除・一覧表示" },
  { href: "/loans", title: "貸出管理", desc: "貸出の実行・返却・履歴確認" },
];

export default function HomePage() {
  return (
    <div>
      <h1 className={styles.title}>図書館管理システム</h1>
      <p className={styles.subtitle}>管理メニューを選択してください</p>
      <div className={styles.grid}>
        {sections.map((s) => (
          <Link href={s.href} key={s.href} className={styles.card}>
            <h2>{s.title}</h2>
            <p>{s.desc}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
