        final JFrame f2=new JFrame("Admin Main");
        f2.setSize(1350,730);
        f2.setVisible(true);
        f1.setVisible(false);
        f2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GridBagLayout gbl=new GridBagLayout();

        final JPanel p2=new JPanel(gbl){
            private Image img = ImageIO.read(new File("F:\\Untitled Folder\\Rohan\\football2.jpg"));
        @Override
        protected void paintComponent( Graphics g ) {
            super.paintComponent(g);

            g.drawImage(img, 0,0,1366,730, null);
            }
        };

        GridBagConstraints g2=new GridBagConstraints();
        g2.insets=new Insets(3,3,3,3);
        JLabel l2=new JLabel("Admin ID",JLabel.LEFT);
        JLabel l3=new JLabel("Password",JLabel.LEFT);
        l2.setFont(new Font("TimesRoman",Font.BOLD,16));
        l2.setForeground(Color.BLUE);
        l2.setBackground(Color.WHITE);
        l3.setFont(new Font("TimesRoman",Font.BOLD,16));
        l3.setForeground(Color.BLUE);
        l3.setBackground(Color.WHITE);
        final JTextField t1=new JTextField(15);
        final JPasswordField pw1=new JPasswordField(15);
        JButton b3=new JButton("Back");
        JButton b4=new JButton("Sign in");
        f2.add(p2);
        g2.anchor=GridBagConstraints.FIRST_LINE_END;
        g2.gridx=1;
        g2.gridy=1;
        p2.add(l2,g2);
        g2.gridx=2;
        g2.gridy=1;
        p2.add(t1,g2);
        g2.gridx=1;
        g2.gridy=2;
        p2.add(l3,g2);
        g2.gridx=2;
        g2.gridy=2;
        p2.add(pw1,g2);
        g2.gridx=1;
        g2.gridy=3;
        p2.add(b3,g2);
        g2.gridx=2;
        g2.gridy=3;
        p2.add(b4,g2);