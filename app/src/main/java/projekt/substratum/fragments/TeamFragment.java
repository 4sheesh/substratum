package projekt.substratum.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import projekt.substratum.R;

/**
 * Created by Nicholas on 2016-03-31.
 */
public class TeamFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.team_fragment, null);

        CardView nicholas = (CardView) root.findViewById(R.id.nicholas);
        nicholas.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_nicholas_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView syko = (CardView) root.findViewById(R.id.syko);
        syko.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_syko_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView george = (CardView) root.findViewById(R.id.george);
        george.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_george_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView cory = (CardView) root.findViewById(R.id.cory);
        cory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_cory_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView branden = (CardView) root.findViewById(R.id.branden);
        branden.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_branden_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView dave = (CardView) root.findViewById(R.id.dave);
        dave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_dave_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView jimmy = (CardView) root.findViewById(R.id.jimmy);
        jimmy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_jimmy_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        return root;
    }
}